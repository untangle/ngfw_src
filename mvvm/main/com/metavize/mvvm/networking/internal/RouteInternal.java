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

package com.metavize.mvvm.networking.internal;

import java.util.List;

import com.metavize.mvvm.tran.IPaddr;

import com.metavize.mvvm.networking.IPNetwork;

import com.metavize.mvvm.networking.Route;

/** An immutable routing entry */
public class RouteInternal
{
    /**
     * Presently network space is only used to display routes that are
     * immutable
     */
    private final NetworkSpaceInternal networkSpace;
    
    private final IPNetwork destination;
    private final IPaddr nextHop;

    private RouteInternal( Route route, NetworkSpaceInternal networkSpace )
    {
        this.networkSpace = networkSpace;
        this.destination = route.getDestination();
        this.nextHop = route.getNextHop();
    }
    
    /** The network space this route belongs to, this will not be
     * supported until =RELEASE3.3=. */
    // public NetworkSpaceInternal getNetworkSpace()
    // {
    // return this.networkSpace;
    // }

    /** destination network that triggers this routing entry. */
    public IPNetwork getDestination()
    {
        return this.destination;
    }

    /** The IP address of the next router. */
    public IPaddr getNextHop()
    {
        return this.nextHop;
    }

    public Route toRoute()
    {
        return new Route( null, this.destination, this.nextHop );
    }

    public String toString()
    {
        return
            "destination: " + getDestination() +
            "\nnext-hop:    " + getNextHop();
    }

    public static RouteInternal makeInstance( Route route )
    {
        return new RouteInternal( route, null );
    }

    
}
