/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */


package com.metavize.jnetcap;

import java.net.InetAddress;
import java.net.Inet4Address;

public class SubscriptionEndpoint
{
    private static final InterfaceSet DEFAULT_INTERFACE_SET;
    private static final InetAddress  DEFAULT_ADDRESS;
    private static final InetAddress  DEFAULT_NETMASK;
    private static final Range        DEFAULT_PORT;

    private InterfaceSet interfaceSet;
    private InetAddress  address;
    private InetAddress  netmask;
    private Range        port;

    public SubscriptionEndpoint() 
    {
        interfaceSet = DEFAULT_INTERFACE_SET;
        address      = DEFAULT_ADDRESS;
        netmask      = DEFAULT_NETMASK;
        port         = DEFAULT_PORT;
    }
    
    /**
     * Retrieve the interface set
     */
    public  InterfaceSet interfaceSet()
    {
        return interfaceSet;
    }

    /**
     * Retrieve the addresses for this subscription
     */
    public  InetAddress  address()
    {
        return address; 
    }

    /**
     * Retrieve the netmask for this subscription
     */
    public  InetAddress  netmask() 
    {
        return netmask;
    }

    /**
     * Retrieve the port range for this subscription
     */
    public  Range        port()
    {
        return port; 
    }

    /**
     * Set the interface set for this subscription
     * @param interfaceSet - Set of interfaces to subscribe to.
     */
    public void interfaceSet( InterfaceSet interfaceSet ) 
    {
        this.interfaceSet = interfaceSet; 
    }

    /**
     * Set the address for this subscription 
     * @param address - The new address of the subscription.
     */
    public void address( InetAddress address ) 
    {
        this.address = address; 
    }

    /**
     * Set the netmask for this subscription
     * @param netmask - The new netmask.
     */
    public void netmask( InetAddress netmask ) 
    { 
        this.netmask = netmask; 
    }

    /**
     * Set the port for this endpoint to a range of values
     * @param port - Port range to subscribe to.
     */
    public void port( Range port ) 
    { 
        this.port = port; 
    }

    /**
     * Set the port for this endpoint to a specific value.</p>
     * @param port - Port between 1 and 65535
     */
    public void port( int port ) throws JNetcapException
    {
        if ( port < 1 || port > 0xFFFF ) {
            throw new JNetcapException( "Invalid port value: " + port );
        }
        this.port = new Range( port, port ); 
    }

    static 
    {
        Range range = null;

        try {
            range = new Range( 1, 0xFFFF );
        } catch ( JNetcapException e ) {
            /* Logger may not exist, this should really never happen */
            System.out.println( "ERROR: Subscription Endpoint unable to initialize default range." );
            range = null;
        }

        DEFAULT_INTERFACE_SET = new InterfaceSet();
        DEFAULT_ADDRESS       = Inet4AddressConverter.getByAddress( new int[] { 0, 0, 0, 0 } );
        DEFAULT_NETMASK       = DEFAULT_ADDRESS;
        DEFAULT_PORT          = range;
    }
}

