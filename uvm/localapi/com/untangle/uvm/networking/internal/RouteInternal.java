/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.networking.internal;

import java.util.List;

import com.untangle.uvm.node.IPaddr;

import com.untangle.uvm.networking.IPNetwork;

import com.untangle.uvm.networking.Route;

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
    
    /* values from a rule */
    private final boolean isEnabled;
    private final String name;
    private final String category;
    private final String description;


    private RouteInternal( Route route, NetworkSpaceInternal networkSpace )
    {
        this.networkSpace = networkSpace;
        this.destination = route.getDestination();
        this.nextHop = route.getNextHop();
        
        this.isEnabled = route.isLive();
        this.name = route.getName();
        this.category = route.getCategory();
        this.description = route.getDescription();
    }
    
    /** The network space this route belongs to, this will not be
     * supported until =RELEASE3.3=. */
    // public NetworkSpaceInternal getNetworkSpace()
    // {
    // return this.networkSpace;
    // }

    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }

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
        Route route = new Route( null, this.destination, this.nextHop );
        route.setLive( getIsEnabled());
        route.setName( getName());
        route.setDescription( getDescription());
        route.setCategory( getCategory());
        return route;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public String getCategory()
    {
        return this.category;
    }

    public String toString()
    {
        return
            "name:        "   + getName() +
            "\ndestination: " + getDestination() +
            "\nnext-hop:    " + getNextHop();
    }

    public static RouteInternal makeInstance( Route route )
    {
        return new RouteInternal( route, null );
    }

    
}
