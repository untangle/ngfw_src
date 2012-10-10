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
import com.untangle.uvm.node.PenaltyBoxEvent;
import com.untangle.uvm.node.HostTableEvent;

/**
 * HostTable stores a global table of all "local" IPs that have recently been seen.
 * This table is useful for storing information know about the various hosts.
 *
 * Different applications can add known information about various hosts by attaching objects with keys
 * Other applications can check what is known about various hosts by looking up objected stored for the various keys
 */
public class HostTableImpl implements HostTable
{
    private final Logger logger = Logger.getLogger(getClass());

    private Hashtable<InetAddress, HostTableEntry> hostTable;

    private Set<HostTable.PenaltyBoxListener> penaltyBoxListeners = new HashSet<PenaltyBoxListener>();

    private EventLogQuery penaltyBoxEventQuery;
    private EventLogQuery hostTableEventQuery;
    
    protected HostTableImpl()
    {
        this.hostTable = new Hashtable<InetAddress, HostTableEntry>();

        this.penaltyBoxEventQuery = new EventLogQuery(I18nUtil.marktr("PenaltyBox Events"), "SELECT * FROM reports.penaltybox ORDER BY time_stamp DESC");
        this.hostTableEventQuery = new EventLogQuery(I18nUtil.marktr("Host Table Events"), "SELECT * FROM reports.host_table_updates ORDER BY time_stamp DESC");
    }
    
    public void setAttachment( InetAddress addr, String key, Object ob )
    {
        if ( addr == null || key == null ) {
            logger.warn( "Invalid arguments: setAttachment( " + addr + " , " + key + " , " + ob + " )");
            return;
        }

        logger.info("setAttachment( " + addr + " , " + key + " , " + ob + " )");

        HostTableEntry entry = getHostTableEntry( addr, true );

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
            
        logger.debug("getAttachment( " + addr + " , " + key + " )");

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
        else
            return entry.attachments.get( key );
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
                Object value = getAttachment( entry.addr, key );
                if (value != null) 
                    entry.attachments.put( key, value );
            }
        }

        return hosts;
    }
    
    public synchronized void addHostToPenaltyBox( InetAddress address, int priority, int time_sec )
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

        PenaltyBoxEvent evt = new PenaltyBoxEvent( action, address, priority, new Date(currentEntryTime), new Date(currentExitTime) ) ;
        UvmContextFactory.context().logEvent(evt);

        /**
         * Call listeners
         */
        if (action == PenaltyBoxEvent.ACTION_ENTER) {
            for ( PenaltyBoxListener listener : this.penaltyBoxListeners ) {
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
            
        UvmContextFactory.context().logEvent( new PenaltyBoxEvent( PenaltyBoxEvent.ACTION_EXIT, address, 0, entryDate, exitTime ) );

        /**
         * Call listeners
         */
        for ( PenaltyBoxListener listener : this.penaltyBoxListeners ) {
            try {
                listener.exitingPenaltyBox( address );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
        
        return;
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
        Date exitDate = new Date(exitTime);
        Date now = new Date();
        if (now.after(exitDate)) {
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
            if (! UvmContextFactory.context().hostTable().hostInPenaltyBox( entry.getAddr() ) )
                i.remove();
        }

        return list;
    }
    
    public void registerListener( HostTable.PenaltyBoxListener listener )
    {
        this.penaltyBoxListeners.add( listener );
    }

    public void unregisterListener( HostTable.PenaltyBoxListener listener )
    {
        this.penaltyBoxListeners.remove( listener );
    }

    public EventLogQuery[] getPenaltyBoxEventQueries()
    {
        return new EventLogQuery[] { this.penaltyBoxEventQuery };
    }

    public EventLogQuery[] getHostTableEventQueries()
    {
        return new EventLogQuery[] { this.hostTableEventQuery };
    }
    
    private HostTableEntry getHostTableEntry( InetAddress addr, boolean createIfNecessary )
    {
        HostTableEntry entry = hostTable.get( addr );

        if ( entry == null && createIfNecessary ) {
            entry = createNewHostTableEntry( addr );
            hostTable.put( addr, entry );
        }

        return entry;
    }

    private HostTableEntry createNewHostTableEntry( InetAddress addr )
    {
        HostTableEntry entry = new HostTableEntry();

        entry.addr = addr;
        entry.attachments = new Hashtable<String, Object>();
        entry.creationTime = System.currentTimeMillis();
        entry.lastAccessTime = entry.creationTime;

        return entry;
    }

    
}
