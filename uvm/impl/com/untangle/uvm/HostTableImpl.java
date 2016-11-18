/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.PenaltyBoxEvent;
import com.untangle.uvm.node.QuotaEvent;

/**
 * HostTable stores a global table of all "local" IPs that have recently been seen.
 * This table is useful for storing information know about the various hosts.
 *
 * Different applications can add known information about various hosts by attaching objects with keys
 * Other applications can check what is known about various hosts by looking up objected stored for the various keys
 *
 * Other Documentation in HostTable.java
 */
public class HostTableImpl implements HostTable
{
    private static final int HIGH_WATER_SIZE = 12000; /* absolute max */
    private static final int LOW_WATER_SIZE = 10000; /* max size to reduce to when pruning map */

    private static final int CLEANER_SLEEP_TIME_MILLI = 60 * 1000; /* 60 seconds */
    private static final int CLEANER_LAST_ACCESS_MAX_TIME = 60 * 60 * 1000; /* 60 minutes */
    
    private static final String HOSTS_SAVE_FILENAME = System.getProperty("uvm.settings.dir") + "/untangle-vm/hosts.js";

    private static final int PERIODIC_SAVE_DELAY = 1000 * 60 * 60 * 6; /* 6 hours */

    private final Logger logger = Logger.getLogger(getClass());

    private ConcurrentHashMap<InetAddress, HostTableEntry> hostTable;

    private Set<HostTable.HostTableListener> listeners = new HashSet<HostTableListener>();

    private volatile Thread cleanerThread;
    private HostTableCleaner cleaner = new HostTableCleaner();

    private volatile Thread reverseLookupThread;
    private volatile Semaphore reverseLookupSemaphore = new Semaphore(0);
    private HostTableReverseHostnameLookup reverseLookup = new HostTableReverseHostnameLookup();

    private final Pulse saverPulse = new Pulse("device-table-saver", new HostTableSaver(), PERIODIC_SAVE_DELAY);
    
    private int maxActiveSize = 0;

    private volatile long lastSaveTime = 0;
    
    protected HostTableImpl()
    {
        this.lastSaveTime = System.currentTimeMillis();
        loadSavedHosts();
        
        saverPulse.start();

        UvmContextFactory.context().newThread(this.cleaner).start();
        UvmContextFactory.context().newThread(this.reverseLookup).start();
    }
    
    public synchronized void setHosts( LinkedList<HostTableEntry> newHosts )
    {
        ConcurrentHashMap<InetAddress, HostTableEntry> oldHostTable = this.hostTable;
        this.hostTable = new ConcurrentHashMap<InetAddress, HostTableEntry>();
        
        /**
         * For each entry, copy the value on top of the exitsing objects so references are maintained
         * If there aren't in the table, create new entries
         */
        for ( HostTableEntry entry : newHosts ) {
            InetAddress address = entry.getAddress();
            if (address == null)
                continue;

            HostTableEntry existingEntry = oldHostTable.get( address );
            if ( existingEntry != null ) {
                existingEntry.copy( entry );
                this.hostTable.put( existingEntry.getAddress(), existingEntry );
            }
            else {
                this.hostTable.put( address, entry );
                this.reverseLookupSemaphore.release(); /* wake up thread to force hostname lookup */
            }
        }

        saveHosts();
    }

    public HostTableEntry getHostTableEntry( InetAddress addr )
    {
        return getHostTableEntry( addr, false );
    }

    public HostTableEntry getHostTableEntry( InetAddress addr, boolean createIfNecessary )
    {
        HostTableEntry entry = hostTable.get( addr );

        if ( entry == null && createIfNecessary ) {
            entry = createNewHostTableEntry( addr );
            hostTable.put( addr, entry );
            this.reverseLookupSemaphore.release(); /* wake up thread to force hostname lookup */
            UvmContextFactory.context().hookManager().callCallbacks( HookManager.HOST_TABLE_ADD, addr );
        }

        return entry;
    }
    
    public void setHostTableEntry( InetAddress addr, HostTableEntry entry )
    {
        hostTable.put( addr, entry );
    }

    public LinkedList<HostTableEntry> getHosts()
    {
        return new LinkedList<HostTableEntry>(hostTable.values());
    }
    
    public synchronized void addHostToPenaltyBox( InetAddress address, int time_sec, String reason )
    {
        HostTableEntry entry = getHostTableEntry( address, true );
        long entryTime = System.currentTimeMillis();
        long exitTime  = entryTime + (((long)time_sec) * 1000L);

        logger.info("Adding " + address.getHostAddress() + " to Penalty box for " + time_sec + " seconds");

        /**
         * Set Penalty Boxed flag
         */
        boolean currentFlag = entry.getPenaltyBoxed();
        entry.setPenaltyBoxed(true);

        /**
         * If the entry time is null, set it.
         * If it is not null, the host was probably already in the penalty box so don't update it
         */
        long currentEntryTime = entry.getPenaltyBoxEntryTime();
        if (currentEntryTime == 0)
            entry.setPenaltyBoxEntryTime( entryTime );
        currentEntryTime = entryTime;

        /**
         * Update the exit time, if the proposed value is after the current value
         */
        long currentExitTime = entry.getPenaltyBoxExitTime();
        if (currentExitTime == 0 || exitTime > currentExitTime)
            entry.setPenaltyBoxExitTime( exitTime );
        currentExitTime = exitTime;
            
        int action;
        if ( currentFlag ) {
            action = PenaltyBoxEvent.ACTION_REENTER; /* was already there */
        } else {
            action = PenaltyBoxEvent.ACTION_ENTER; /* new entry */
        }

        PenaltyBoxEvent evt = new PenaltyBoxEvent( action, address, new Date(currentEntryTime), new Date(currentExitTime), reason ) ;
        UvmContextFactory.context().logEvent(evt);

        /**
         * Call listeners
         */
        if (action == PenaltyBoxEvent.ACTION_ENTER) {
            for ( HostTableListener listener : this.listeners ) {
                try {
                    listener.enteringPenaltyBox( address );
                } catch ( Exception e ) {
                    logger.error( "Exception calling listener", e );
                }
            }
        }
        
        return;
    }

    public synchronized void releaseHostFromPenaltyBox( InetAddress address )
    {
        HostTableEntry entry = getHostTableEntry( address );
        if (entry == null) /* host not in penalty box */
            return;
        
        long now = System.currentTimeMillis();

        /**
         * Save previous values and remove from penalty box
         */
        boolean currentFlag = entry.getPenaltyBoxed();
        long currentEntryTime = entry.getPenaltyBoxEntryTime();
        long currentExitTime  = entry.getPenaltyBoxExitTime();
        entry.setPenaltyBoxed( false );
        entry.setPenaltyBoxEntryTime( 0 );
        entry.setPenaltyBoxExitTime( 0 );
        
        /**
         * If the host was in the penalty box, we must log the event and call the listeners
         */

        if ( !currentFlag ) {
            return;
        }
        if ( currentEntryTime == 0 ) {
            logger.warn("Entry time not set for penalty boxed host");
            return;
        }
        if ( currentExitTime == 0 ) {
            logger.warn("Exit time not set for penalty boxed host");
            return;
        }
        
        /**
         * If current date is before planned exit time, use it instead, otherwise just log the exit time
         */
        if ( now > currentExitTime ) {
            logger.info("Removing " + address.getHostAddress() + " from Penalty box. (expired)");
        } else {
            logger.info("Removing " + address.getHostAddress() + " from Penalty box. (admin requested)");
            currentExitTime = now; /* set exitTime to now, because the host was release prematurely */
        }
            
        UvmContextFactory.context().logEvent( new PenaltyBoxEvent( PenaltyBoxEvent.ACTION_EXIT, address, new Date(currentEntryTime), new Date(currentExitTime), null ) );

        /**
         * Call listeners
         */
        for ( HostTableListener listener : this.listeners ) {
            try {
                listener.exitingPenaltyBox( address );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
        
        return;
    }

    public synchronized void giveHostQuota( InetAddress address, long quotaBytes, int time_sec, String reason )
    {
        if (address == null) {
            logger.warn("Invalid argument: address is null");
            return;
        }
        HostTableEntry entry = getHostTableEntry( address, true );
        long now = System.currentTimeMillis();

        /* If there already is a quota and it will be reset */
        entry.setQuotaSize( quotaBytes );
        entry.setQuotaRemaining( quotaBytes );
        entry.setQuotaIssueTime( now );
        entry.setQuotaExpirationTime( now + (((long)time_sec)*1000L) );

        /**
         * Call listeners
         */
        for ( HostTableListener listener : this.listeners ) {
            try {
                listener.quotaGiven( address );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }

        UvmContextFactory.context().logEvent( new QuotaEvent( QuotaEvent.ACTION_GIVEN, address, reason, quotaBytes ) );
        
        return;
    }

    public synchronized void removeQuota( InetAddress address )
    {
        if (address == null) {
            logger.warn("Invalid argument: address is null");
            return;
        }
        HostTableEntry entry = getHostTableEntry( address );
        if (entry == null)
            return;

        entry.setQuotaSize( 0 );
        entry.setQuotaRemaining( 0 );
        entry.setQuotaIssueTime( 0 );
        entry.setQuotaExpirationTime( 0 );

        /**
         * Call listeners
         */
        for ( HostTableListener listener : this.listeners ) {
            try {
                listener.quotaRemoved( address );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
    }

    public boolean hostQuotaExceeded( InetAddress address )
    {
        if (address == null) {
            logger.warn("Invalid argument: address is null");
            return false;
        }
        HostTableEntry entry = getHostTableEntry( address );
        if ( entry == null )
            return false;
        if (entry.getQuotaSize() <= 0)
            return false;

        /**
         * Check if its expired, if it is - remove the quota
         */
        long now = System.currentTimeMillis();
        if (now > entry.getQuotaExpirationTime()) {
            removeQuota( address );
            return false;
        }

        if (entry.getQuotaRemaining() <= 0)
            return true;
        return false;
    }

    public double hostQuotaAttainment( InetAddress address )
    {
        if (address == null) {
            logger.warn("Invalid argument: address is null");
            return 0.0;
        }
        HostTableEntry entry = getHostTableEntry( address );
        if ( entry == null )
            return 0.0;
        if (entry.getQuotaSize() <= 0)
            return 0.0;

        /**
         * Check if its expired, if it is - remove the quota
         */
        long now = System.currentTimeMillis();
        if (now > entry.getQuotaExpirationTime()) {
            removeQuota( address );
            return 0.0;
        }
        
        long quotaRemaining = entry.getQuotaRemaining();
        long quotaSize = entry.getQuotaSize();
        long quotaUsed = quotaSize - quotaRemaining;
        
        long quotaUsedK = quotaUsed/1000;
        long quotaSizeK = quotaSize/1000;

        return ((double)quotaUsedK)/((double)quotaSizeK);
    }
    
    public synchronized void refillQuota(InetAddress address)
    {
        if (address == null) {
            logger.warn("Invalid argument: address is null");
            return;
        }
        HostTableEntry entry = getHostTableEntry( address );
        if ( entry == null )
            return;
        if ( entry.getQuotaSize() <= 0 )
            return;

        entry.setQuotaRemaining( entry.getQuotaSize() );

        for ( HostTableListener listener : this.listeners ) {
            try {
                listener.quotaGiven( address );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
    }

    public synchronized boolean decrementQuota( InetAddress address, long bytes )
    {
        if (address == null) {
            logger.warn("Invalid argument: address is null");
            return false;
        }
        HostTableEntry entry = getHostTableEntry( address );
        if ( entry == null )
            return false;
        if ( entry.getQuotaSize() <= 0 )
            return false;

        /**
         * Decrement
         */
        long remaining = entry.getQuotaRemaining();
        long newRemaning = remaining - bytes;
        entry.setQuotaRemaining( newRemaning );


        if ( remaining > 0 && newRemaning <= 0 ) {
            logger.info("Host " + address.getHostAddress() + " exceeded quota.");

            for ( HostTableListener listener : this.listeners ) {
                try {
                    listener.quotaExceeded( address );
                } catch ( Exception e ) {
                    logger.error( "Exception calling listener", e );
                }
            }
            
            UvmContextFactory.context().logEvent( new QuotaEvent( QuotaEvent.ACTION_EXCEEDED, address, null, entry.getQuotaSize()) );
            return true;
        }

        return false;
    }
    
    public boolean hostInPenaltyBox( InetAddress address )
    {
        if (address == null) {
            logger.warn("Invalid argument: address is null");
            return false;
        }
        HostTableEntry entry = getHostTableEntry( address );
        if ( entry == null )
            return false;
        if ( !entry.getPenaltyBoxed() )
            return false;

        /**
         * If the exit time has already passed the host is no longer penalty boxed
         * As such, release the host from the penalty box immediately and return false
         */
        long exitTime = entry.getPenaltyBoxExitTime();
        long now = System.currentTimeMillis();
        if ( exitTime != 0 && now > exitTime ) {
            releaseHostFromPenaltyBox( address );
            return false;
        }
                
        return true;
    }

    public LinkedList<HostTableEntry> getPenaltyBoxedHosts()
    {
        LinkedList<HostTableEntry> list = new LinkedList<HostTableEntry>(UvmContextFactory.context().hostTable().getHosts());

        for (Iterator<HostTableEntry> i = list.iterator(); i.hasNext(); ) {
            HostTableEntry entry = i.next();
            if (! hostInPenaltyBox( entry.getAddress() ) )
                i.remove();
        }

        return list;
    }

    public LinkedList<HostTableEntry> getQuotaHosts()
    {
        LinkedList<HostTableEntry> list = new LinkedList<HostTableEntry>(UvmContextFactory.context().hostTable().getHosts());

        for (Iterator<HostTableEntry> i = list.iterator(); i.hasNext(); ) {
            HostTableEntry entry = i.next();
            if ( entry.getQuotaSize() <= 0 )
                i.remove();
        }

        return list;
    }
    
    public void registerListener( HostTable.HostTableListener listener )
    {
        this.listeners.add( listener );
    }

    public void unregisterListener( HostTable.HostTableListener listener )
    {
        this.listeners.remove( listener );
    }

    public int getCurrentSize()
    {
        return this.hostTable.size();
    }

    public int getCurrentActiveSize()
    {
        int licenseSize = 0;

        /**
         * Only count hosts with getLastSessionTime() is > 0
         * Meaning the UVM has processed sessions for that host
         * and its processes sessions within 24 hours
         */
        try {
            for ( Iterator<HostTableEntry> i = hostTable.values().iterator() ; i.hasNext() ; ) {
                HostTableEntry entry = i.next();
                /* Only count hosts that are "active" */
                if ( entry.getActive() )
                    licenseSize++;
            }
        }
        catch (java.util.ConcurrentModificationException e) {} // ignore this, just best effort
        
        return licenseSize;
    }
    
    public int getMaxActiveSize()
    {
        return this.maxActiveSize;
    }

    public void clearTable()
    {
        this.hostTable.clear();
    }

    public HostTableEntry removeHostTableEntry( InetAddress address )
    {
        if ( address == null ) {
            logger.warn( "Invalid argument: " + address );
            return null;
        }
        logger.info("Removing host table entry: " + address.getHostAddress());

        HostTableEvent event = new HostTableEvent( address, "remove", null );
        UvmContextFactory.context().logEvent(event);

        HostTableEntry removed =  hostTable.remove( address );
        UvmContextFactory.context().hookManager().callCallbacks( HookManager.HOST_TABLE_REMOVE, address );
        return removed;
    }
    
    private synchronized HostTableEntry createNewHostTableEntry( InetAddress address )
    {
        HostTableEntry entry = new HostTableEntry();

        HostTableEvent event = new HostTableEvent( address, "add", null );
        UvmContextFactory.context().logEvent(event);
        
        entry.setAddress( address );

        checkForDevice( entry, address );
        
        int seatLimit = UvmContextFactory.context().licenseManager().getSeatLimit();
        int currentSize = getCurrentActiveSize();
        
        // if there is a seat limit, and the size of the table is currently greater than that seatLimit
        // this host is out of compliance and not entitled
        if ( seatLimit > 0 && currentSize > seatLimit ) {
            entry.setEntitled( false );
        }
        
        return entry;
    }

    /**
     * This funciton checks for a matching entry in the device table.
     * If it does not exist, it adds it
     */
    private void checkForDevice( HostTableEntry entry, InetAddress address )
    {
        String macAddress = UvmContextFactory.context().netcapManager().arpLookup( address.getHostAddress() );
        if ( macAddress == null )
            return;
        if ( "".equals(macAddress) )
            return;
        
        entry.setMacAddress( macAddress );
        DeviceTableEntry deviceEntry = UvmContextFactory.context().deviceTable().getDevice( macAddress );

        /**
         * If this device has never been seen before, add it
         */
        if ( deviceEntry == null )
            deviceEntry = UvmContextFactory.context().deviceTable().addDevice( macAddress );

        /**
         * Restore known information from the device entry where able
         */
        if ( deviceEntry.getHostname() != null )
            entry.setHostname( deviceEntry.getHostname() );
        if ( deviceEntry.getDeviceUsername() != null )
            entry.setUsernameDevice( deviceEntry.getDeviceUsername() );
        if ( deviceEntry.getHttpUserAgent() != null )
            entry.setHttpUserAgent( deviceEntry.getHttpUserAgent() );
        if ( deviceEntry.getMacVendor() != null )
            entry.setMacVendor( deviceEntry.getMacVendor() );
    }
    
    @SuppressWarnings("unchecked")
    public void saveHosts()
    {
        lastSaveTime = System.currentTimeMillis();
        
        try {
            Collection<HostTableEntry> entries = hostTable.values();
            logger.info("Saving hosts to file... (" + entries.size() + " entries)");

            LinkedList<HostTableEntry> list = new LinkedList<HostTableEntry>();
            for ( HostTableEntry entry : entries ) { list.add(entry); }

            if (list.size() > HIGH_WATER_SIZE) {
                logger.info("Host list over max size, pruning oldest entries"); // remove entries with oldest (lowest) lastSeenTime
                Collections.sort( list, new Comparator<HostTableEntry>() { public int compare(HostTableEntry o1, HostTableEntry o2) {
                    if ( o1.getLastAccessTime() < o2.getLastAccessTime() ) return 1;
                    if ( o1.getLastAccessTime() == o2.getLastAccessTime() ) return 0;
                    return -1;
                } });
                while ( list.size() > LOW_WATER_SIZE ) {
                    logger.info("Host list too large. Removing oldest entry: " + list.get(list.size()-1));
                    list.removeLast();
                }
            }
            
            UvmContextFactory.context().settingsManager().save( HOSTS_SAVE_FILENAME, list, false, true );
            logger.info("Saving hosts to file... done");
        } catch (Exception e) {
            logger.warn("Exception",e);
        }
    }

    private void adjustMaxSizeIfNecessary()
    {
        int realSize = 0;

        for ( Iterator<HostTableEntry> i = hostTable.values().iterator() ; i.hasNext() ; ) {
            HostTableEntry entry = i.next();
            /* Only count hosts that are "active" */
            if ( entry.getActive() )
                realSize++;
        }
        
        if (realSize > this.maxActiveSize)
            this.maxActiveSize = realSize;
    }

    @SuppressWarnings("unchecked")
    private void loadSavedHosts()
    {
        try {
            this.hostTable = new ConcurrentHashMap<InetAddress,HostTableEntry>();

            logger.info("Loading hosts from file...");
            LinkedList<HostTableEntry> savedEntries = UvmContextFactory.context().settingsManager().load( LinkedList.class, HOSTS_SAVE_FILENAME );
            if ( savedEntries == null ) {
                logger.info("Loaded  hosts from file.   (no hosts saved)");
            } else {
                for ( HostTableEntry entry : savedEntries ) {
                    try {
                        // if its invalid just ignore it
                        if ( entry.getAddress() == null ) {
                            logger.warn("Invalid entry: " + entry.toJSONString());
                            continue;
                        }

                        checkForDevice( entry, entry.getAddress() );
                        
                        hostTable.put( entry.getAddress(), entry );
                    } catch ( Exception e ) {
                        logger.warn( "Error loading host entry: " + entry.toJSONString(), e);
                    }
                }
                logger.info("Loaded  hosts from file.   (" + savedEntries.size() + " entries)");
            }
        } catch (Exception e) {
            logger.warn("Failed to load hosts",e);
        }
    }
    
    /**
     * This thread periodically walks through the entries and removes expired entries
     * It also explicitly releases hosts from the penalty box and quotas after expiration
     */
    private class HostTableCleaner implements Runnable
    {
        public void run()
        {
            cleanerThread = Thread.currentThread();

            while (cleanerThread != null) {
                try {Thread.sleep(CLEANER_SLEEP_TIME_MILLI);} catch (Exception e) {}
                logger.debug("HostTableCleaner: Running... ");

                try {
                    Long now = System.currentTimeMillis();
                    /**
                     * Remove old entries
                     */
                    LinkedList<HostTableEntry> entries = new LinkedList<HostTableEntry>(hostTable.values());
                    for (HostTableEntry entry : entries) {
                        InetAddress address = entry.getAddress();
                        if ( address == null )
                            continue;
                        String addressStr = address.getHostAddress();

                        /**
                         * Check if the MAC address for this host has changed
                         * If so, delete it from the host table so all state will be cleared.
                         */
                        try {
                            String macAddress1 = entry.getMacAddress();
                            if ( macAddress1 != null && !"".equals(macAddress1) ) {
                                String macAddress2 = UvmContextFactory.context().netcapManager().arpLookup( addressStr );
                                if ( macAddress2 != null && !macAddress2.equals("") ) {
                                    if ( !macAddress1.equals(macAddress2) ) {
                                        logger.warn("Host " + addressStr + " changed MAC address " + macAddress1 + " -> " + macAddress2 + ". Deleting host entry...");

                                        removeHostTableEntry( address );
                                        continue;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Exception",e);
                        }
                        
                        /**
                         * Check penalty box expiration
                         * Remove from penalty box if expired
                         */
                        if ( entry.getPenaltyBoxed() ) {
                            long exitTime = entry.getPenaltyBoxExitTime();
                            if (now > exitTime) {
                                releaseHostFromPenaltyBox( address );
                            }
                        }

                        /**
                         * Check quota expiration
                         * Remove from quota if expired
                         */
                        if ( entry.getQuotaSize() > 0 ) {
                            long expireTime = entry.getQuotaExpirationTime();
                            if ( now > expireTime ) {
                                removeQuota( address );
                            }
                        }

                        /**
                         * If this host hasnt been touched recently, delete it
                         */
                        if ( now > (entry.getLastAccessTime() + CLEANER_LAST_ACCESS_MAX_TIME) ) {

                            /**
                             * If this host table entry is storing vital information, don't delete it
                             */
                            if ( entry.getQuotaSize() > 0 ||
                                 entry.getPenaltyBoxed() ||
                                 entry.getCaptivePortalAuthenticated() /* check authenticated flag instead of username because anonymous logins don't set username */
                                 ) {
                                continue;
                            }
                            /**
                             * If this host is still reachable/online, don't remove the information
                             * Limit this check to 3 hops TTL and 500 ms. Sometimes external (but pingable) hosts can get into the host table
                             * via spoofing and/or SSDP and UPnP and stuff like that.
                             * We want these hosts to expire once we don't have traffic for them.
                             * This should ensure that only *local* pingable addresses stay in the host table if they respond to ping.
                             */
                            if ( entry.getAddress().isReachable( null, 3, 500 ) ) {
                                continue;
                            }
                            
                            /**
                             * Otherwise just delete the entire entry
                             */
                            else {
                                logger.debug("HostTableCleaner: Removing " + address.getHostAddress());

                                removeHostTableEntry( address );
                                continue;
                            }
                        }
                    }

                    /**
                     * if certain hosts are "unlicensed" and show now be entitled, set them back
                     */
                    int numUnlicensed = 0;
                    entries = new LinkedList<HostTableEntry>(hostTable.values());
                    for (HostTableEntry entry : entries) {
                        if (!entry.getEntitled())
                            numUnlicensed++;
                    }
                    if ( UvmContextFactory.context().licenseManager() != null ) {
                        int seatLimit = UvmContextFactory.context().licenseManager().getSeatLimit();
                        int excess = hostTable.size() - seatLimit;
                        // if there number of unlicensed hosts is more than it should be - reduce it
                        if ( numUnlicensed > excess ) {
                            int reduction = numUnlicensed - excess;
                            for (HostTableEntry entry : entries) {
                                if (!entry.getEntitled()) {
                                    entry.setEntitled( true );
                                    reduction--;
                                    if ( reduction < 1 )
                                        break;
                                }
                            }
                        }
                    }
                    
                    adjustMaxSizeIfNecessary();
                    
                } catch (Exception e) {
                    logger.warn("Exception while cleaning host table",e);
                }
            }
        }
    }

    /**
     * This thread periodically walks through the entries and does a reverse lookup on the IP 
     * to see if it can determine the host's hostname (for hosts without a known hostname)
     *
     * This is done in a separate thread because it may hang on the DNS lookup.
     */
    private class HostTableReverseHostnameLookup implements Runnable
    {
        public void run()
        {
            reverseLookupThread = Thread.currentThread();

            try {
                org.xbill.DNS.ExtendedResolver defaultResolver = new org.xbill.DNS.ExtendedResolver(new String[]{"127.0.0.1"});
                defaultResolver.setTimeout(2);
                org.xbill.DNS.Lookup.setDefaultResolver(defaultResolver);
            } catch (Exception e) {
                logger.warn("Exception:",e);
            }

            while (reverseLookupThread != null) {

                reverseLookupSemaphore.drainPermits();
                try {reverseLookupSemaphore.tryAcquire(CLEANER_SLEEP_TIME_MILLI,java.util.concurrent.TimeUnit.MILLISECONDS);} catch (Exception e) {}
                logger.debug("HostTableReverseHostnameLookup: Running... ");

                try {
                    LinkedList<HostTableEntry> entries = new LinkedList<HostTableEntry>(hostTable.values());
                    for (HostTableEntry entry : entries) {

                        String currentHostname = entry.getHostname();
                        InetAddress address = entry.getAddress();

                        checkForDevice( entry, address );

                        if ( address == null ) {
                            if ( logger.isDebugEnabled() )
                                logger.debug("HostTableReverseHostnameLookup: Skipping " + address + " - null");
                            continue;
                        }
                        if ( entry.isHostnameKnown() ) {
                            if ( logger.isDebugEnabled() )
                                logger.debug("HostTableReverseHostnameLookup: Skipping " + address.getHostAddress() + " - already known hostname: " + currentHostname);
                            continue;
                        }
                        
                        try {
                            String hostname = org.xbill.DNS.Address.getHostName(address);

                            if ( hostname == null ) {
                                if ( logger.isDebugEnabled() )
                                    logger.debug("HostTableReverseHostnameLookup: Skipping " + address.getHostAddress() + " - lookup failed.");
                                continue;
                            }
                            if ( hostname.equals( currentHostname ) ) {
                                if ( logger.isDebugEnabled() )
                                    logger.debug("HostTableReverseHostnameLookup: Skipping " + address.getHostAddress() + " - lookup result same as current:" + currentHostname);
                                continue;
                            }
                            if ( hostname.equals( address.getHostAddress() ) ) {
                                if ( logger.isDebugEnabled() )
                                    logger.debug("HostTableReverseHostnameLookup: Skipping " + address.getHostAddress() + " - lookup results is address:" + address.getHostAddress());
                                continue;
                            }

                            /* use just the first part of the name */
                            int firstdot = hostname.indexOf('.');
                            if ( firstdot != -1 )
                                hostname = hostname.substring(0,firstdot);
                            
                            logger.debug("HostTable Reverse lookup hostname = " + hostname);
                            entry.setHostname( hostname );
                        } catch (java.net.UnknownHostException e ) {
                            //do nothing
                        } catch (Exception e) {
                            logger.warn("Exception in reverse lookup",e);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Exception while doing reverse lookups:",e);
                }
            }
        }
    }

    private class HostTableSaver implements Runnable
    {
        public void run()
        {
            saveHosts();
        }
    }
    
}
