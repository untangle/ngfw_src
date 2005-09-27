/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import com.metavize.mvvm.ConnectivityTester;
import org.apache.log4j.Logger;

class ConnectivityTesterImpl implements ConnectivityTester
{
    private static final Logger logger = Logger.getLogger( ConnectivityTesterImpl.class );

    /* Name of the host to lookup */
    private static final String TEST_HOSTNAME_BASE    = "release";
    private static final String TEST_HOSTNAME_DOMAIN  = "metavize.com";

    /* Backup IP address to use if DNS fails */
    private static final String BACKUP_ADDRESS_STRING = "216.129.106.56";

    /* Address to use if the DNS lookup fails */
    private static final InetAddress BACKUP_ADDRESS;

    /* Port the TCP test will try to connect to */
    private static final int TCP_TEST_PORT = 80;

    /* The amount of time before giving up on the DNS attempt in milliseconds */
    private static final int DNS_TEST_TIMEOUT_MS = 10000;
    private static final int TCP_TEST_TIMEOUT_MS = 10000;

    private static final Random RANDOM = new Random();

    private static ConnectivityTesterImpl INSTANCE = new ConnectivityTesterImpl();

    /* Address of release */
    private InetAddress address;

    /**
     * Retrieve the connectivity tester
     */
    public Status getStatus()
    {
        /* Returns the lookuped address if DNS is working, or null if it is not */
        return ConnectionStatus.makeConnectionStatus( isDnsWorking(), isTcpWorking());
    }

    /**
     * Test that DNS is working
     */
    private boolean isDnsWorking()
    {
        DnsTest dnsTest = new DnsTest();
        Thread test = new Thread( dnsTest );

        test.start();

        try {
            test.join( DNS_TEST_TIMEOUT_MS );
            if ( test.isAlive()) {
                test.interrupt();
            }
        } catch( InterruptedException e ) {
            logger.error( "Interrupted while testing DNS connectivity.", e );
        }

        this.address = dnsTest.address;

        return dnsTest.isWorking;
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

    class DnsTest implements Runnable
    {
        InetAddress address = null;
        boolean isWorking = false;

        public DnsTest()
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

            this.isWorking = false;

            /* Try a random host that doesn't exist.    *
             * the negative response should be returned *
             * immediately */
            try {
                String host = TEST_HOSTNAME_BASE + "-" + RANDOM.nextInt() + "." + TEST_HOSTNAME_DOMAIN;
                InetAddress invalidAddress;
                logger.debug( "Looking up invalid address: " +  host );
                invalidAddress = InetAddress.getByName( host );
                System.out.println( "Finished the bad lookup" );
                this.isWorking = false;
            } catch ( UnknownHostException e ) {
                this.isWorking = true;
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
                this.isWorking = true;
                logger.debug( "Completed TCP Connection test" );
            } catch ( IOException e ) {
                this.isWorking = false;
                logger.warn( "Unable to connect to " + this.address );
            }
        }
    }
}

class ConnectionStatus implements ConnectivityTester.Status
{
    private static final ConnectionStatus DNS_AND_TCP = new ConnectionStatus( true, true );
    private static final ConnectionStatus DNS         = new ConnectionStatus( true, false );
    private static final ConnectionStatus TCP         = new ConnectionStatus( false, true );
    private static final ConnectionStatus NOTHING     = new ConnectionStatus( false, false );

    private final boolean isDnsWorking;
    private final boolean isTcpWorking;

    ConnectionStatus( boolean isDnsWorking, boolean isTcpWorking )
    {
            this.isDnsWorking = isDnsWorking;
            this.isTcpWorking = isTcpWorking;
    }

    public boolean isTcpWorking()
    {
        return this.isTcpWorking;
    }

    public boolean isDnsWorking()
    {
        return this.isDnsWorking;
    }

    static ConnectionStatus makeConnectionStatus( boolean isDnsWorking, boolean isTcpWorking )
    {
        if ( isDnsWorking && isTcpWorking ) {
            return DNS_AND_TCP;
        } else if ( isDnsWorking ) {
            return DNS;
        } else if ( isTcpWorking ) {
            return TCP;
        }
        return NOTHING;
    }
}
