/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ConnectivityTester;
import com.untangle.uvm.networking.NetworkConfiguration;
import com.untangle.uvm.networking.ConnectionStatus;
import com.untangle.uvm.networking.InterfaceConfiguration;

class ConnectivityTesterImpl implements ConnectivityTester
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String UVM_BASE  = System.getProperty( "uvm.home" );
    private static final String DNS_TEST_SCRIPT    = UVM_BASE + "/bin/ut-dns-test";
    private static final String BRIDGE_WAIT_SCRIPT = UVM_BASE + "/bin/ut-bridge-wait";

    /* Name of the host to lookup */
    private static final String TEST_HOSTNAME_BASE    = "updates";
    private static final String TEST_HOSTNAME_DOMAIN  = "untangle.com";

    /* Backup IP address to use if DNS fails */
    private static final String BACKUP_ADDRESS_STRING = "74.123.28.44";

    /* Address to use if the DNS lookup fails */
    private static final InetAddress BACKUP_ADDRESS;

    /* Port the TCP test will try to connect to */
    private static final int TCP_TEST_PORT = 80;

    /* The amount of time before giving up on the DNS attempt in milliseconds */
    private static final int DNS_TEST_TIMEOUT_MS = 5000;
    private static final int TCP_TEST_TIMEOUT_MS = 10000;

    /* Give the bridge 30 seconds to come alive */
    private static final int BRIDGE_WAIT_TIMEOUT = 30000;

    private static ConnectivityTesterImpl INSTANCE = new ConnectivityTesterImpl();

    /* Address of updates */
    private InetAddress address;

    /**
     * Retrieve the connectivity tester
     */
    public Status getStatus()
    {
        NetworkConfiguration networkSettings = UvmContextFactory.context().networkManager().getNetworkConfiguration();
        InterfaceConfiguration wan = networkSettings.findFirstWAN();
        
        InetAddress dnsPrimary   = wan.getDns1();
        InetAddress dnsSecondary = wan.getDns2();

        /* Wait for any bridge interfaces to come up */
        waitForBridges();

        /* Returns the lookuped address if DNS is working, or null if it is not */
        return ConnectionStatus.makeConnectionStatus( isDnsWorking( dnsPrimary, dnsSecondary ), isTcpWorking());
    }

    /**
     * Test that DNS is working
     */
    public boolean isDnsWorking( InetAddress dnsPrimaryServer, InetAddress dnsSecondaryServer )
    {
        boolean isWorking = false;
        String primaryServer = null;
        String secondaryServer = null;

        if ( dnsPrimaryServer != null) primaryServer = dnsPrimaryServer.getHostAddress();
        if ( dnsSecondaryServer != null ) secondaryServer = dnsSecondaryServer.getHostAddress();

        if ( primaryServer != null && UvmContextFactory.context().execManager().execResult(DNS_TEST_SCRIPT + " " + primaryServer ) == 0)
            isWorking = true;
        if ( secondaryServer != null && UvmContextFactory.context().execManager().execResult(DNS_TEST_SCRIPT + " " + secondaryServer ) == 0)
            isWorking = true;
            
        /* Now run the dns test just to get the address of updates */
        DnsLookup dnsLookup = new DnsLookup();
        Thread test = new Thread( dnsLookup );

        test.start();

        try {
            test.join( DNS_TEST_TIMEOUT_MS );
            if ( test.isAlive()) test.interrupt();
        } catch( InterruptedException e ) {
            logger.error( "Interrupted while testing DNS connectivity.", e );
        }

        this.address = dnsLookup.address;

        return isWorking;
    }

    /**
     * Wait until a bridge comes up
     */
    private void waitForBridges()
    {
        try {
            Thread  bridge = new Thread( new Runnable() {
                    public void run()
                    {
                        try {
                            UvmContextFactory.context().execManager().exec( BRIDGE_WAIT_SCRIPT + " "  + BRIDGE_WAIT_TIMEOUT);
                        } catch ( Exception e ) {
                            logger.info( "Exception executing bridge wait script", e );
                        }
                    }
                } );

            bridge.start();

            bridge.join( BRIDGE_WAIT_TIMEOUT + 1000 );
            if ( bridge.isAlive()) bridge.interrupt();
        } catch ( Exception e ) {
            logger.warn( "unable to determine bridge status" );
        }
    }

    /**
     * Test that TCP is working
     */
    private boolean isTcpWorking()
    {
        InetAddress testAddress;
        if ( this.address == null ) {
            testAddress = BACKUP_ADDRESS;
        } else {
            testAddress = this.address;
        }

        TcpTest tcpTest = new TcpTest( testAddress );

        Thread test = new Thread( tcpTest );

        test.start();

        try {
            test.join( TCP_TEST_TIMEOUT_MS );
            if ( test.isAlive()) {
                test.interrupt();
            }
        } catch( InterruptedException e ) {
            logger.error( "Interrupted while testing TCP connectivity.", e );
        }

        return tcpTest.isWorking;
    }

    static ConnectivityTesterImpl getInstance()
    {
        return INSTANCE;
    }

    static {
        InetAddress address = null;

        try {
            address = InetAddress.getByName( BACKUP_ADDRESS_STRING );
        } catch ( UnknownHostException e ) {
            System.err.println( "!!!! This should never happen" + e );
            address = null;
        }

        BACKUP_ADDRESS = address;
    }

    /* This isn't a test, it is just a method used to lookup the address
     * of updates.untangle.com with a timeout.  The real test is now executed by
     * the script.  For the original test, look at subversion R2828 */
    class DnsLookup implements Runnable
    {
        InetAddress address = null;

        public DnsLookup()
        {
        }

        public void run()
        {
            /* This always works after the first time, so it doesn't actually do anything */
            try {
                String host = TEST_HOSTNAME_BASE + "." + TEST_HOSTNAME_DOMAIN;
                logger.debug( "Starting lookup" );
                this.address = InetAddress.getByName( host );
                logger.debug( "Found address: " + address );
                logger.debug( "Completed lookup" );
            } catch ( UnknownHostException e ) {
                this.address   = null;
                logger.warn( "Unable to look up host: " + TEST_HOSTNAME_BASE + "." + TEST_HOSTNAME_DOMAIN );
            }
        }
    }

    class TcpTest implements Runnable
    {
        private final InetAddress address ;
        boolean isWorking = false;

        public TcpTest( InetAddress address )
        {
            this.address = address;
        }

        public void run()
        {
            try {
                logger.debug( "Trying to connect to " + this.address );
                Socket socket = new Socket( this.address, TCP_TEST_PORT );
                socket.close();
                this.isWorking = true;
                logger.debug( "Completed TCP Connection test" );
            } catch ( IOException e ) {
                this.isWorking = false;
                logger.warn( "Unable to connect to " + this.address );
            }
        }
    }
}

