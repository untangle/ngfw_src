/**
 * $Id$
 */
package com.untangle.app.router;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.DeviceTableEntry;
import org.apache.log4j.Logger;

/**
 * This monitors the DHCP status file and maintains a map of existing DHCP entries
 */
class DhcpMonitor implements Runnable
{
    /* How often to poll the leases file, in milliseconds */
    private static final long   SLEEP_TIME              = 1000;

    private static final int    DHCP_LEASE_ENTRY_LENGTH = 5;
    private static final String DHCP_LEASES_FILE        = "/var/lib/misc/dnsmasq.leases";
    private static final String DHCP_LEASE_DELIM        = " ";
    private static final String DHCP_EMPTY_HOSTNAME     = "*";
    private static final int    DHCP_LEASE_ENTRY_EOL    = 0;
    private static final int    DHCP_LEASE_ENTRY_MAC    = 1;
    private static final int    DHCP_LEASE_ENTRY_IP     = 2;
    private static final int    DHCP_LEASE_ENTRY_HOST   = 3;

    private static final Date   NEVER                   = new Date ( Long.MAX_VALUE );

    /* The thread the monitor is running on */
    private Thread thread = null;

    /* Status of the monitor */
    private volatile boolean isAlive = true;

    /* File being monitored */
    private final File dhcpFile = new File( DHCP_LEASES_FILE );

    /* Last time the event log was updated */
    private long lastUpdate = -1L;

    /* Next time that a lease is supposed to expire.  If there are no changes to a file
     * but a lease expires, then the leases must be reparsed */
    private Date nextExpiration = NEVER;

    /**
     * A map of all of the leases being tracked.  Has to be concurrent
     * in order for another thread to look up addresses.
     */
    private final Map<InetAddress,DhcpLease> currentLeaseMap = new ConcurrentHashMap<InetAddress,DhcpLease>();

    private final Logger logger = Logger.getLogger( this.getClass());
    private final RouterImpl app;

    /**
     * DhcpMonitor - create a DHCP monitor
     * @param app
     */
    public DhcpMonitor( RouterImpl app )
    {
        this.app = app;
    }

    /**
     * Call this run the DHCP monitor
     * This does not return until the DHCP monitor is killed
     */
    public void run()
    {
        logger.debug( "Starting" );

        while ( true ) {
            try {
                Thread.sleep( SLEEP_TIME );
            } catch ( InterruptedException e ) {}

            /* Check if the app is still running */
            if ( !isAlive ) {
                break;
            }

            try {
                /* The time right now to determine if leases have been expired */
                Date now = new Date();

                if ( hasDhcpChanged( now ))
                    parseDhcpFile( now );
            } catch ( SecurityException e ) {
                logger.error( "SecurityException when accessing file: " + DHCP_LEASES_FILE );
            }
        }

        logger.debug( "Finished" );
    }
    
    /**
     * lookupLease
     * Retrieve the current lease for an IP address
     * @param address
     * @return DhcpLease
     */
    protected DhcpLease lookupLease( InetAddress address )
    {
        return currentLeaseMap.get( address );
    }

    /**
     * lookupHostname
     * @param address
     * @return the hostname
     */
    protected String lookupHostname( InetAddress address )
    {
        DhcpLease lease = lookupLease( address );
        if (lease != null)
            return lease.getHostname();
        return null;
    }
    
    /**
     * start the DHCP monitor
     */
    protected synchronized void start()
    {
        isAlive = true;

        logger.debug( "Starting thread" );

        /* If thread is not-null, there is a running thread that thinks it is alive */
        if ( thread != null ) {
            logger.debug( "Monitor is already running" );
            return;
        }

        thread = UvmContextFactory.context().newThread( this );
        thread.start();
    }

    /**
     * stop the dhcp monitor
     */
    protected synchronized void stop()
    {
        if ( thread != null ) {
            logger.debug( "Stopping thread" );

            isAlive = false;
            try {
                thread.interrupt();
                thread.join( SLEEP_TIME * 3 );
            } catch ( SecurityException e ) {
                logger.error( "security exception, impossible", e );
            } catch ( InterruptedException e ) {
                logger.error( "interrupted while stopping", e );
            }
            thread = null;
        } else {
            logger.debug( "Monitor is already stopped." );
        }
    }

    /**
     * hasDhcpChanged - returns true if DHCP has changed since the specified time
     * @param now
     * @return true if changed, false otherwise
     */
    private boolean hasDhcpChanged( Date now )
    {
        if ( lastUpdate < dhcpFile.lastModified()) return true;
        if ( now.after( nextExpiration )) return true;
        return false;
    }

    /**
     * parseDhcpFile
     * @param now
     * @throws SecurityException
     */
    private void parseDhcpFile( Date now ) throws SecurityException
    {
        BufferedReader in = null;

        Set<InetAddress> deletedSet = new HashSet<InetAddress>( currentLeaseMap.keySet() );

        /* If there are no leases, this should never expire */
        nextExpiration = NEVER;

        try {
            in = new BufferedReader(new FileReader( DHCP_LEASES_FILE ));

            String str;
            while (( str = in.readLine()) != null ) {
                parseLease( str, now, deletedSet );
            }

        } catch ( FileNotFoundException ex ) {
            logger.info( "The file: " + DHCP_LEASES_FILE + " does not exist yet" );
        } catch ( Exception ex ) {
            logger.error( "Error reading file: " + DHCP_LEASES_FILE, ex );
        } finally  {
            try {
                if ( in != null ) in.close();
            } catch ( Exception ex ) {
                logger.error( "Error closing file: " + DHCP_LEASES_FILE, ex );
            }
        }

        for ( Iterator<InetAddress> iter = deletedSet.iterator() ; iter.hasNext() ; ) {
            InetAddress ip = iter.next();
            DhcpLease lease = currentLeaseMap.remove( ip );
            if ( lease == null ) {
                logger.error( "Logic error item only in deleted set " + ip.getHostAddress());
            }
        }

        /* Update the last time the file was modified */
        lastUpdate = now.getTime();
    }

    /**
     * parseLease
     * @param str
     * @param now
     * @param deletedSet
     */
    private void parseLease( String str, Date now, Set<InetAddress> deletedSet )
    {
        str = str.trim();
        String strArray[] = str.split( DHCP_LEASE_DELIM );
        String tmp;
        String host;
        Date eol;
        String mac;
        InetAddress ip;

        if ( strArray.length != DHCP_LEASE_ENTRY_LENGTH ) {
            logger.error( "Invalid DHCP lease: " + str );
            return;
        }

        tmp  = strArray[DHCP_LEASE_ENTRY_EOL];
        try {
            eol = new Date( Long.parseLong( tmp ) * 1000 );
        } catch ( NumberFormatException e ) {
            logger.error( "Invalid DHCP date: " + tmp );
            return;
        }

        mac  = strArray[DHCP_LEASE_ENTRY_MAC];

        tmp  = strArray[DHCP_LEASE_ENTRY_IP];
        try {
            ip = InetAddress.getByName( tmp );
        } catch ( Exception e ) {
            logger.error( "Invalid IP address: " + tmp, e );
            return;
        }

        tmp = strArray[DHCP_LEASE_ENTRY_HOST];
        if ( tmp.equals( DHCP_EMPTY_HOSTNAME )) {
            host = "";
        } else {
            host = tmp;
        }

        processDhcpLease( eol, mac, ip, host, now );

        /* Remove the item from the set of deleted items */
        deletedSet.remove( ip );
    }

    /**
     * processDhcpLease
     * @param eol
     * @param mac
     * @param ip
     * @param host
     * @param now
     */
    private void processDhcpLease( Date eol, String mac, InetAddress ip, String host, Date now )
    {
        /* Determine if this lease is already being tracked */
        DhcpLease lease = currentLeaseMap.get( ip );

        if ( lease == null ) {
            /* Add the lease to the map */
            lease = new DhcpLease( eol, mac, ip, host, now );
            logger.info("Adding DHCP Lease: " + ip.getHostAddress());
            currentLeaseMap.put( ip, lease );
        } else {

            if ( lease.hasChanged( eol, mac, ip, host, now )) {
                /* must update the lease here because the previous values are determine
                 * whether this is a release or renew */
                logger.info("Updating DHCP Lease: " + ip.getHostAddress());
                lease.set( eol, mac, ip, host, now );
            } else {
                logger.debug( "Lease hasn't changed: " + ip.toString());
            }
        }

        /**
         * Update nextExpiration
         */
        if ( lease.isActive() && nextExpiration.after( eol ))
            nextExpiration = eol;

        /**
         * Lets do some sanity checks
         * Lookup the host entry for this IP and check the mac address.
         * If it has changed go ahead and forget all state about that host.
         */
        // HostTableEntry hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( ip );
        // //DeviceTableEntry deviceEntry = UvmContextFactory.context().deviceTable().getDevice( mac );
        // if ( hostEntry != null ) {
        //     String currentMac = hostEntry.getMacAddress();
        //     if ( currentMac != null && !currentMac.equals("") ) {
        //         if ( !currentMac.equals( mac ) ) {
        //             logger.warn("Host " + ip + " changed MAC address " + currentMac + " -> " + mac + ". Deleting host entry...");
        //             UvmContextFactory.context().hostTable().removeHostTableEntry( ip );
        //         }
        //     }
        // }
    }
}
