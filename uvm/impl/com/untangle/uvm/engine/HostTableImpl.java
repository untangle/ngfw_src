/**
 * $Id: HostTableImpl.java,v 1.00 2012/08/29 10:12:07 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import java.util.Iterator;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.HostTableEvent;
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
    private static final int CLEANER_SLEEP_TIME_MILLI = 60 * 1000; /* 60 seconds */
    private static final int CLEANER_LAST_ACCESS_MAX_TIME = 5 * 60 * 1000; /* 5 minutes */

    private final Logger logger = Logger.getLogger(getClass());

    private Hashtable<InetAddress, HostTableEntry> hostTable;

    private Set<HostTable.HostTableListener> listeners = new HashSet<HostTableListener>();

    private EventLogQuery penaltyBoxEventQuery;
    private EventLogQuery hostTableEventQuery;
    private EventLogQuery quotaEventQuery;

    private volatile Thread cleanerThread;
    private HostTableCleaner cleaner = new HostTableCleaner();

    private volatile Thread reverseLookupThread;
    private HostTableReverseHostnameLookup reverseLookup = new HostTableReverseHostnameLookup();
    
    protected HostTableImpl()
    {
        this.hostTable = new Hashtable<InetAddress, HostTableEntry>();

        this.penaltyBoxEventQuery = new EventLogQuery(I18nUtil.marktr("PenaltyBox Events"), "SELECT * FROM reports.penaltybox ORDER BY time_stamp DESC");
        this.hostTableEventQuery = new EventLogQuery(I18nUtil.marktr("Host Table Events"), "SELECT * FROM reports.host_table_updates ORDER BY time_stamp DESC");
        this.quotaEventQuery = new EventLogQuery(I18nUtil.marktr("Quota Events"), "SELECT * FROM reports.quotas ORDER BY time_stamp DESC");

        UvmContextFactory.context().newThread(this.cleaner).start();
        UvmContextFactory.context().newThread(this.reverseLookup).start();
    }
    
    public void setAttachment( InetAddress addr, String key, String str )
    {
        setAttachment( addr, key, (Object)str);
    }

    public void setAttachment( InetAddress addr, String key, Object ob )
    {
        if ( addr == null || key == null ) {
            logger.warn( "Invalid arguments: setAttachment( " + addr + " , " + key + " , " + ob + " )");
            return;
        }

        logger.info("setAttachment( " + addr.getHostAddress() + " , " + key + " , " + ob + " )");

        HostTableEntry entry = getHostTableEntry( addr, true );

        entry.lastAccessTime = System.currentTimeMillis();
        
        if (ob != null)
            entry.attachments.put( key, ob );
        else
            entry.attachments.remove( key ); /* if null, remove object */

        if (ob == null)
            UvmContextFactory.context().logEvent(new HostTableEvent( addr, key, null ) );
        else
            UvmContextFactory.context().logEvent(new HostTableEvent( addr, key, ob.toString() ) );
        
        return;
    }

    public Object getAttachment( InetAddress addr, String key )
    {
        if ( addr == null || key == null ) {
            logger.warn( "Invalid arguments: getAttachment( " + addr + " , " + key + " )");
            return null;
        }
            
        logger.debug("getAttachment( " + addr.getHostAddress() + " , " + key + " )");

        /**
         * Special treatment for USERNAME
         */
        if (key.equals(HostTable.KEY_USERNAME)) {
            String capture_username = (String) getAttachment( addr, HostTable.KEY_CAPTURE_USERNAME );
            if (capture_username != null)
                return capture_username;
            String adconnector_username = (String) getAttachment( addr, HostTable.KEY_ADCONNECTOR_USERNAME );
            if (adconnector_username != null)
                return adconnector_username;
        }
        /**
         * Special treatment for USERNAME_SOURCE
         */
        if (key.equals(HostTable.KEY_USERNAME_SOURCE)) {
            String capture_username = (String) getAttachment( addr, HostTable.KEY_CAPTURE_USERNAME );
            if (capture_username != null)
                return "capture";
            String adconnector_username = (String) getAttachment( addr, HostTable.KEY_ADCONNECTOR_USERNAME );
            if (adconnector_username != null)
                return "adconnector";
        }

        /**
         * just return whatever is found in the table
         */
        HostTableEntry entry = getHostTableEntry( addr, false );
        if (entry == null)
            return null;
        else {
            entry.lastAccessTime = System.currentTimeMillis();
            return entry.attachments.get( key );
        }
    }

    public String[] getPossibleAttachments()
    {
        return HostTable.ALL_ATTACHMENTS;
    }

    public LinkedList<HostTableEntry> getHosts()
    {
        LinkedList<HostTableEntry> hosts = new LinkedList<HostTableEntry>(hostTable.values());

        for (HostTableEntry entry: hosts) {
            /**
             * create a copy of the hash table so the original can not be modified
             * Iterate through all keys, this handles non-persistent keys like USERNAME
             */
            entry.attachments = new Hashtable<String, Object>(entry.attachments); 
            for ( String key : HostTable.ALL_ATTACHMENTS) {
                Object value = getAttachment( entry.address, key );
                if (value != null) 
                    entry.attachments.put( key, value );
            }
        }

        return hosts;
    }
    
    public synchronized void addHostToPenaltyBox( InetAddress address, int priority, int time_sec, String reason )
    {
        Long entryTime = System.currentTimeMillis();
        Long exitTime  = entryTime + (time_sec * 1000L);

        logger.info("Adding " + address.getHostAddress() + " to Penalty box for " + time_sec + " seconds");

        /**
         * Set PENALTY_BOXED boolean to true
         */
        Boolean currentFlag = (Boolean) getAttachment( address, HostTable.KEY_PENALTY_BOXED );
        setAttachment( address, HostTable.KEY_PENALTY_BOXED, Boolean.TRUE );
        setAttachment( address, HostTable.KEY_PENALTY_BOX_PRIORITY, Integer.valueOf(priority) );

        /**
         * If the entry time is null, set it.
         * If it is not null, the host was probably already in the penalty box so don't update it
         */
        Long currentEntryTime = (Long) getAttachment( address, HostTable.KEY_PENALTY_BOX_ENTRY_TIME );
        if (currentEntryTime == null)
            setAttachment( address, HostTable.KEY_PENALTY_BOX_ENTRY_TIME, entryTime );
        currentEntryTime = (Long) getAttachment( address, HostTable.KEY_PENALTY_BOX_ENTRY_TIME );

        /**
         * Update the exit time, if the proposed value is after the current value
         */
        Long currentExitTime = (Long) getAttachment( address, HostTable.KEY_PENALTY_BOX_EXIT_TIME );
        if (currentExitTime == null || exitTime > currentExitTime)
            setAttachment( address, HostTable.KEY_PENALTY_BOX_EXIT_TIME, exitTime );
        currentExitTime = (Long) getAttachment( address, HostTable.KEY_PENALTY_BOX_EXIT_TIME );
            
        int action;
        if (currentFlag != null && currentFlag ) {
            action = PenaltyBoxEvent.ACTION_REENTER; /* was already there */
        } else {
            action = PenaltyBoxEvent.ACTION_ENTER; /* new entry */
        }

        PenaltyBoxEvent evt = new PenaltyBoxEvent( action, address, priority, new Date(currentEntryTime), new Date(currentExitTime), reason ) ;
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
        Date now = new Date();

        /**
         * Set PENALTY_BOXED boolean to false
         */
        Boolean currentFlag = (Boolean) getAttachment( address, HostTable.KEY_PENALTY_BOXED );
        Long currentEntryTime = (Long) getAttachment( address, HostTable.KEY_PENALTY_BOX_ENTRY_TIME );
        Long currentExitTime = (Long) getAttachment( address, HostTable.KEY_PENALTY_BOX_EXIT_TIME );

        setAttachment( address, HostTable.KEY_PENALTY_BOXED, null );
        setAttachment( address, HostTable.KEY_PENALTY_BOX_PRIORITY, null );
        setAttachment( address, HostTable.KEY_PENALTY_BOX_ENTRY_TIME, null );
        setAttachment( address, HostTable.KEY_PENALTY_BOX_EXIT_TIME, null );
            
        /**
         * If the host is not in the penalty box, just return
         */
        if ( currentFlag == null || currentFlag == Boolean.FALSE )
            return;
            
        if ( currentEntryTime == null) {
            logger.warn("Entry time not set for penalty boxed host");
            return;
        }
        if ( currentExitTime == null) {
            logger.warn("Exit time not set for penalty boxed host");
            return;
        }
        
        Date entryDate = new Date(currentEntryTime);
        Date exitTime = new Date(currentExitTime);
        
        /**
         * If current date is before planned exit time, use it instead, otherwise just log the exit time
         */
        if ( now.after( exitTime ) ) {
            logger.info("Removing " + address.getHostAddress() + " from Penalty box. (expired)");
        } else {
            logger.info("Removing " + address.getHostAddress() + " from Penalty box. (admin requested)");
            exitTime = now; /* set exitTime to now, because the host was release prematurely */
        }
            
        UvmContextFactory.context().logEvent( new PenaltyBoxEvent( PenaltyBoxEvent.ACTION_EXIT, address, 0, entryDate, exitTime, null ) );

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

        long now = System.currentTimeMillis();

        /* If there already is a quota and it will be reset */
        setAttachment( address, HostTable.KEY_QUOTA_SIZE, quotaBytes );
        setAttachment( address, HostTable.KEY_QUOTA_REMAINING, quotaBytes );
        setAttachment( address, HostTable.KEY_QUOTA_ISSUE_TIME, now );
        setAttachment( address, HostTable.KEY_QUOTA_EXPIRATION_TIME, (now + (time_sec*1000)) );

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

        setAttachment( address, HostTable.KEY_QUOTA_SIZE, null );
        setAttachment( address, HostTable.KEY_QUOTA_REMAINING, null );
        setAttachment( address, HostTable.KEY_QUOTA_ISSUE_TIME, null );
        setAttachment( address, HostTable.KEY_QUOTA_EXPIRATION_TIME, null );

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

        Long currentQuotaExpiration = (Long) getAttachment( address, HostTable.KEY_QUOTA_EXPIRATION_TIME );
        long now = System.currentTimeMillis();

        if (currentQuotaExpiration == null)
            return false;
        if (now > currentQuotaExpiration) {
            removeQuota( address );
            return false;
        }

        Long remaining = (Long) getAttachment( address, HostTable.KEY_QUOTA_REMAINING );
        if (remaining == null) {
            logger.warn("Missing quota remaining value");
            return false;
        }

        if (remaining <= 0)
            return true;
        return false;
    }

    public synchronized void refillQuota(InetAddress address)
    {
        if (address == null) {
            logger.warn("Invalid argument: address is null");
            return;
        }

        Long currentQuotaSize = (Long) getAttachment( address, HostTable.KEY_QUOTA_SIZE );

        if (currentQuotaSize == null) {
            logger.warn("Quota not found: " + address);
            return;
        }

        setAttachment( address, HostTable.KEY_QUOTA_REMAINING, currentQuotaSize );
    }

    public synchronized boolean decrementQuota(InetAddress addr, long bytes)
    {
        Long remaining = (Long) getAttachment( addr, HostTable.KEY_QUOTA_REMAINING );
        if (remaining != null) {
            Long newRemaning = remaining - bytes;
            setAttachment( addr, HostTable.KEY_QUOTA_REMAINING, newRemaning );

            if (remaining > 0 && newRemaning <= 0) {
                Long original = (Long) getAttachment( addr, HostTable.KEY_QUOTA_SIZE );
                logger.info("Host " + addr.getHostAddress() + " exceeded quota.");

                UvmContextFactory.context().logEvent( new QuotaEvent( QuotaEvent.ACTION_EXCEEDED, addr, null, original) );
                return true;
            }
        }

        return false;
    }
    
    public boolean hostInPenaltyBox( InetAddress address )
    {
        Boolean currentFlag = (Boolean) getAttachment( address, HostTable.KEY_PENALTY_BOXED );

        if (currentFlag == null || currentFlag == Boolean.FALSE)
            return false;

        /**
         * If the exit time has already passed the host is no longer penalty boxed
         * As such, release the host from the penalty box immediately and return false
         */
        Long exitTime = (Long) getAttachment( address, HostTable.KEY_PENALTY_BOX_EXIT_TIME );
        Long now = System.currentTimeMillis();
        if (exitTime == null || now > exitTime) {
            releaseHostFromPenaltyBox( address );
            return false;
        }
                
        return true;
    }

    public LinkedList<HostTable.HostTableEntry> getPenaltyBoxedHosts()
    {
        LinkedList<HostTable.HostTableEntry> list = new LinkedList<HostTable.HostTableEntry>(UvmContextFactory.context().hostTable().getHosts());

        for (Iterator i = list.iterator(); i.hasNext(); ) {
            HostTable.HostTableEntry entry = (HostTable.HostTableEntry) i.next();
            if (! UvmContextFactory.context().hostTable().hostInPenaltyBox( entry.getAddress() ) )
                i.remove();
        }

        return list;
    }

    public LinkedList<HostTable.HostTableEntry> getQuotaHosts()
    {
        LinkedList<HostTable.HostTableEntry> list = new LinkedList<HostTable.HostTableEntry>(UvmContextFactory.context().hostTable().getHosts());

        for (Iterator i = list.iterator(); i.hasNext(); ) {
            HostTable.HostTableEntry entry = (HostTable.HostTableEntry) i.next();
            Long quotaSize = (Long) getAttachment( entry.getAddress(), HostTable.KEY_QUOTA_SIZE );
            if ( quotaSize == null )
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

    public EventLogQuery[] getHostTableEventQueries()
    {
        return new EventLogQuery[] { this.hostTableEventQuery };
    }

    public EventLogQuery[] getPenaltyBoxEventQueries()
    {
        return new EventLogQuery[] { this.penaltyBoxEventQuery };
    }

    public EventLogQuery[] getQuotaEventQueries()
    {
        return new EventLogQuery[] { this.quotaEventQuery };
    }
    
    private HostTableEntry getHostTableEntry( InetAddress addr, boolean createIfNecessary )
    {
        HostTableEntry entry = hostTable.get( addr );

        if ( entry == null && createIfNecessary ) {
            entry = createNewHostTableEntry( addr );
            hostTable.put( addr, entry );
            this.reverseLookupThread.interrupt(); /* wake it up to force hostname lookup */
        }

        return entry;
    }

    private HostTableEntry createNewHostTableEntry( InetAddress address )
    {
        HostTableEntry entry = new HostTableEntry();

        entry.address = address;
        entry.attachments = new Hashtable<String, Object>();
        entry.creationTime = System.currentTimeMillis();
        entry.lastAccessTime = entry.creationTime;

        return entry;
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
                    LinkedList<InetAddress> keys = new LinkedList<InetAddress>(hostTable.keySet());
                    for (InetAddress addr : keys) {
                        HostTableEntry entry = getHostTableEntry( addr, false );
                        if ( entry == null )
                            continue;

                        /**
                         * Check penalty box expiration
                         * Remove from penalty box if expired
                         */
                        Boolean penaltyBoxed = (Boolean) getAttachment( addr, HostTable.KEY_PENALTY_BOXED );
                        if (penaltyBoxed != null && penaltyBoxed == Boolean.TRUE) {
                            Long exitTime = (Long) getAttachment( addr, HostTable.KEY_PENALTY_BOX_EXIT_TIME );
                            if (exitTime == null || now > exitTime) {
                                releaseHostFromPenaltyBox( addr );
                            }
                        }

                        /**
                         * Check quota expiration
                         * Remove from quota if expired
                         */
                        Long currentQuotaSize = (Long) getAttachment( addr, HostTable.KEY_QUOTA_SIZE );
                        if (currentQuotaSize != null) {
                            Long currentQuotaExpiration = (Long) getAttachment( addr, HostTable.KEY_QUOTA_EXPIRATION_TIME );
                            if (currentQuotaExpiration == null || now > currentQuotaExpiration) {
                                removeQuota( addr );
                            }
                        }

                        /**
                         * Don't remove host that are penalty boxed or have quotas
                         */
                        if (currentQuotaSize != null || penaltyBoxed != null)
                            continue;
                        
                        /**
                         * If this host hasnt been touched recently, delete it
                         */
                        if ( now > (entry.lastAccessTime + CLEANER_LAST_ACCESS_MAX_TIME) ) {
                            logger.debug("HostTableCleaner: Removing " + addr.getHostAddress());
                            hostTable.remove(addr);
                        }
                    }
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

            while (reverseLookupThread != null) {
                try {Thread.sleep(CLEANER_SLEEP_TIME_MILLI);} catch (Exception e) {}
                logger.debug("HostTableReverseHostnameLookup: Running... ");

                try {
                    Long now = System.currentTimeMillis();
                    /**
                     * Remove old entries
                     */
                    LinkedList<InetAddress> keys = new LinkedList<InetAddress>(hostTable.keySet());
                    for (InetAddress addr : keys) {
                        if ( addr == null )
                            continue;
                        String currentHostname = (String) UvmContextFactory.context().hostTable().getAttachment( addr, HostTable.KEY_HOSTNAME );
                        /* if hostname is already known via some other method (and its not just the IP), dont bother doing reverse lookup */
                        if (currentHostname != null && !currentHostname.equals(addr.getHostAddress()) )
                            continue;
                        
                        try {
                            String hostname = addr.getHostName();

                            if ( hostname == null )
                                continue;
                            if ( hostname.equals( currentHostname ) )
                                continue;
                            if ( hostname.equals( addr.getHostAddress() ) )
                                continue;

                            /* use just the first part of the name */
                            int firstdot = hostname.indexOf('.');
                            if ( firstdot != -1 )
                                hostname = hostname.substring(0,firstdot);
                            
                            logger.debug("HostTable Reverse lookup hostname = " + hostname);
                            UvmContextFactory.context().hostTable().setAttachment( addr, HostTable.KEY_HOSTNAME, hostname );
                        } catch (Exception e) {
                            logger.warn("Exception in reverse lookup",e);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Exception while cleaning host table",e);
                }
            }
        }
    }
    
}
