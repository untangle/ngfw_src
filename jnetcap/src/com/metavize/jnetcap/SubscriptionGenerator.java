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

public class SubscriptionGenerator
{
    public static final int SERVER_UNFINISHED    = 0x001;
    public static final int CLIENT_UNFINISHED    = 0x002;
    public static final int ANTI_SUBSCRIBE       = 0x004;
    public static final int BLOCK_CURRENT        = 0x008;
    public static final int LOCAL_ANTI_SUBSCRIBE = 0x010;
    public static final int IS_FAKE              = 0x020;
    public static final int IS_LOCAL             = 0x040;
    public static final int NO_REVERSE           = 0x080;

    public static final int DEFAULT_FLAGS        = SERVER_UNFINISHED | CLIENT_UNFINISHED | BLOCK_CURRENT;

    public static final int PROTOCOL_ALL         = 0xFFFF;

    private int   protocol;
    private int   flags;

    private final SubscriptionEndpoint client;
    private final SubscriptionEndpoint server;

    /**
     * Create a new subscription, by default, all of the subscription generators
     * subscribe to everything on <code>protocol</code>.  The user then must use client 
     * and server to narrow the subscription.
     */
    public SubscriptionGenerator( int protocol, int flags )
    {
        client = new SubscriptionEndpoint();
        server = new SubscriptionEndpoint();

        protocol( protocol );
        flags( flags );
    }
    
    public SubscriptionGenerator( int protocol )
    {
        this( protocol, DEFAULT_FLAGS );
    }

    /**
     * Retrieve the endpoint to configure and view the client parameters for this 
     * subscription generator.
     */
    public SubscriptionEndpoint client() { return client; }

    /**
     * Retrieve the endpoint to configure and view the server parameters for this 
     * subscription generator.
     */
    public SubscriptionEndpoint server() { return server; }

    /**
     * Retrieve the protocol for this subscription generator.
     */
    public int protocol() { return protocol; }

    /**
     * Change the protocol for this subscription generator.
     */
    public void protocol( int protocol ) { 
        /* Verify that protocol is valid */
        if ( protocol != PROTOCOL_ALL ) Netcap.verifyProtocol( protocol );
        
        this.protocol = protocol;
    }

    /**
     * Retrieve the flags for this subscription generator
     */
    public int flags() { return flags; }

    /**
     * Set the flags for this subscription generator.</p>
     * @param flags - The new flags to be used for all subscriptions created from this generator.
     */
    public void flags( int flags ) { this.flags = flags; }

    /**
     * Create a subscription with the current parameters.</p>
     */
    public Subscription subscribe() 
    {
        int id;
        id = createSubscription( this.flags, this.protocol,
                                 this.client.interfaceSet().toInt(),
                                 Inet4AddressConverter.toLong( this.client.address()),
                                 Inet4AddressConverter.toLong( this.client.netmask()),
                                 this.client.port().low(), this.client.port().high(),
                                 this.server.interfaceSet().toInt(),
                                 Inet4AddressConverter.toLong( this.server.address()),
                                 Inet4AddressConverter.toLong( this.server.netmask()),
                                 this.server.port().low(), this.server.port().high());
        
        if ( id < 0 ) Netcap.error( "Unable to create this subscription" );
        
        return new GeneratorSubscription( id );
    }

    private native int createSubscription( int flags, int protocol, 
                                           int srcInterface, long srcAddress, long srcNetmask,
                                           int srcPortLow, int srcPortHigh, 
                                           int dstInterface, long dstAddress, long dstNetmask,
                                           int dstPortLow, int dstPortHigh );

    private static native int unsubscribe( int id );

    class GeneratorSubscription extends Subscription
    {
        GeneratorSubscription( int id ) 
        {
            super( id );
        }

        /**
         * Unsubscribe this subscription.  This may throw an error
         */
        public void unsubscribe()
        {
            /* Verify that this is subscribed */
            subscribed();
            
            int id = this.id;
            this.id = UNSUBSCRIBED;
            
            if ( SubscriptionGenerator.unsubscribe ( id ) < 0 )
                Netcap.error( "Unable to unsubscribe" );
        }
    }

    static 
    {
        Netcap.load();
    }
}
