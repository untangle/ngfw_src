/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.nat;

import java.util.Iterator;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.MACAddress;

class DhcpMonitor implements Runnable
{    
    private static final int    DHCP_LEASE_ENTRY_LENGTH = 5;
    private static final String DHCP_LEASE_DELIM        = " ";

    /* How often to poll the leases file */
    private static final long   SLEEP_TIME              = 5000;
    
    /* Generate a absolute record every so many iterations */
    private static final int    ABSOLUTE_SKIP_COUNT     = 1440; // At 5 second intervals, this is 2 hours

    private static final String DHCP_LEASES_FILE        = "/var/lib/misc/dnsmasq.leases";
    private static final int DHCP_LEASE_ENTRY_EOL       = 0;
    private static final int DHCP_LEASE_ENTRY_MAC       = 1;
    private static final int DHCP_LEASE_ENTRY_IP        = 2;
    private static final int DHCP_LEASE_ENTRY_HOST      = 3;
        
    /* The thread the monitor is running on */
    private final Thread thread;
    
    /* Status of the monitor */
    private boolean isAlive = true;

    /* File being monitored */
    private final File dhcpFile = new File( DHCP_LEASES_FILE );

    /* Last time the event log was updated */
    private long lastUpdate = -1L;
    
    /* Next time that a lease is supposed to expire.  If there are no changes to a file
     * but a lease expires, then the leases must be reparsed */
    private Date nextExpiration = new Date();

    /**
     * A map of all of the leases being tracked.
     */
    private final Map<IPaddr,DhcpLease> currentLeaseMap = new HashMap<IPaddr,DhcpLease>();
    
    private final Logger logger = Logger.getLogger( this.getClass());
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();
    
    public DhcpMonitor( Thread thread )
    {
        this.thread = thread;
    }
    
    public void run( )
    {
        logger.debug( "Starting" );
        int c = 0;

        while ( true ) {            
            if ( c <= 0 ) {
                logAbsolute();
                c = ABSOLUTE_SKIP_COUNT;
            }
            
            try { 
                Thread.sleep( SLEEP_TIME );
            } catch ( InterruptedException e ) {
                logger.info( "Interrupted: ", e );
            }
                        
            /* Check if the transform is still running */
            if ( !isAlive ) {
                break;
            }
            
            c--;
            
            try {
                /* The time right now to determine if leases have been expired */
                Date now = new Date();

                if ( hasDhcpChanged( now )) {
                    logChanges( now, false );
                }
            } catch ( SecurityException e ) {
                logger.error( "SecurityException when accessing file: " + DHCP_LEASES_FILE );
            }
        }

        logger.debug( "Finished" );

        /* XXX Write an absolute lease map that is empty */
    }

    void stop()  throws SecurityException
    {
        isAlive = false;
        thread.interrupt();
    }

    private boolean hasDhcpChanged( Date now ) throws SecurityException
    {
        if ( lastUpdate < dhcpFile.lastModified()) return true;
        if ( now.after( nextExpiration )) return true;
        return false;
    }

    private void logAbsolute() throws SecurityException
    {
        /* Clear out all of the leases */
        currentLeaseMap.clear();
        
        logChanges( new Date(), true );
    }

    private void logChanges( Date now, boolean isAbsolute ) throws SecurityException
    {
        BufferedReader in = null;

        Set<IPaddr> deletedSet = new HashSet( currentLeaseMap.keySet());

        try {
            in = new BufferedReader(new FileReader( DHCP_LEASES_FILE ));
            String str;
            while (( str = in.readLine()) != null ) {
                logLease( str, now, isAbsolute, deletedSet );
            }
        } catch ( Exception ex ) {
            logger.error( "Error reading file: " + DHCP_LEASES_FILE, ex );
        } finally  {
            try { 
                if ( in != null ) in.close();
            } catch ( Exception ex ) {
                logger.error( "Error closing file: " + DHCP_LEASES_FILE, ex );
            }
        }

        for ( Iterator<IPaddr> iter = deletedSet.iterator() ; iter.hasNext() ; ) {
            IPaddr ip = iter.next();
            DhcpLease lease = currentLeaseMap.remove( ip );
            if ( lease == null ) {
                logger.error( "Logic error item only in deleted set " + ip.toString());
            }

            /* XXX Log the item was deleted */
        }
        
        /* Update the last time the file was modified */
        lastUpdate = now.getTime();
    }
        
    private void logLease( String str, Date now, boolean isAbsolute, Set<IPaddr> deletedSet )
    {
        str = str.trim();
        String strArray[] = str.split( DHCP_LEASE_DELIM );
        String tmp;
        HostName host;
        Date eol;
        MACAddress mac;
        IPaddr ip;
        
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
        
        tmp  = strArray[DHCP_LEASE_ENTRY_MAC];
        try {
            mac = MACAddress.parse( tmp );
        } catch ( ParseException e ) {
            logger.error( "Invalid MAC address: " + tmp );
            return;
        }
        
        tmp  = strArray[DHCP_LEASE_ENTRY_IP];
        try {
            ip = IPaddr.parse( tmp );
        } catch ( ParseException e ) {
            logger.error( "Invalid IP address: " + tmp, e );
            return;
        } catch ( UnknownHostException e ) {
            logger.error( "Invalid IP address: " + tmp, e );
            return;
        }
        
        tmp = strArray[DHCP_LEASE_ENTRY_HOST];
        try {
            host = HostName.parse( tmp );
        } catch ( ParseException e ) {
            logger.error( "Invalid hostname: " + tmp );
            return;
        }
        
        /* Determine if this lease is already being tracked */
        DhcpLease lease = currentLeaseMap.get( ip );

        if ( lease == null ) {
            /* Add the lease to the map */
            currentLeaseMap.put( ip, new DhcpLease( eol, mac, ip, host, now ));
            
            /* XXX Log the event */
        } else {
            if ( lease.hasChanged( eol, mac, ip, host, now )) {
                lease.set( eol, mac, ip, host, now );
                /* XXX Log the event */
            }
        }
        
        /* Remove the item from the set of deleted items */
        if (( deletedSet.remove( ip ) == true ) && ( lease == null )) {
            logger.error( "Logic error, ip in delete set but not in lease map " + ip );
        }
    }    
}
