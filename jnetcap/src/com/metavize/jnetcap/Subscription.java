/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: Subscription.java,v 1.7 2005/01/21 01:10:03 rbscott Exp $
 */


package com.metavize.jnetcap;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;

public abstract class Subscription 
{
    protected static final int UNSUBSCRIBED = -1;

    protected int id = UNSUBSCRIBED; /* Netcap subscription id */

    protected Subscription( int id )
    {
        if ( id < 0 ) Netcap.error( "Unable to create a subscription with a negative id: " + id );
        this.id = id;
    }

    public int id() { return id; }

    /**
     * Unsubscribe this subscription.  This may throw an error
     */
    public abstract void unsubscribe();

    public boolean isSubscribed()
    {
        return ( id > 0  ) ? true : false;
    }

    /**
     * Throw an error if a subscription is already subscribed
     * XXX This may no longer be necessary since there is the subscription
     * generator.
     */
    protected void notSubscribed()
    {
        if ( id > 0 ) Netcap.error( "Already subscribed" );
    }

    /**
     * Throw an error if a subscription is not subscribed
     */
    protected void subscribed()
    {
        if ( id < 0 ) Netcap.error( "Not subscribed" );
    }

    static 
    {
        Netcap.load();
    }
}
