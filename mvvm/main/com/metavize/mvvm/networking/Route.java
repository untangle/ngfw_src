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

package com.metavize.mvvm.networking;

import java.util.List;

import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.IPaddr;

/**
 * A routing entry
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="mvvm_network_route"
 */
public class Route extends Rule
{
    /**
     * Presently the network space is only used to display routes that
     * are immutable
     */
    private NetworkSpace networkSpace = null;
    
    private IPNetwork destination;
    private IPaddr nextHop;

    public Route()
    {
    }

    public Route( NetworkSpace networkSpace, IPNetwork destination, IPaddr nextHop )
    {
        this.networkSpace = networkSpace;
        this.destination  = destination;
        this.nextHop      = nextHop;
    }
    
    /**
     * @return The network space this route belongs to, this will not be supported until =RELEASE3.3=.
     * @hibernate.many-to-one
     * cascade="all"
     * class="com.metavize.mvvm.networking.NetworkSpace"
     * column="network_space"
     */
    public NetworkSpace getNetworkSpace()
    {
        return this.networkSpace;
    }

    public void setNetworkSpace( NetworkSpace networkSpace )
    {
        this.networkSpace = networkSpace;
    }

    /**
     * destination network that triggers this routing entry.
     * @return The destination network this route is related to.
     * @hibernate.property
     * type="com.metavize.mvvm.networking.IPNetworkUserType"
     * @hibernate.column
     * name="destination"
     */
    public IPNetwork getDestination()
    {
        if ( this.destination == null ) this.destination = IPNetwork.getEmptyNetwork();
        return this.destination;
    }

    public void setDestination( IPNetwork newValue )
    {
        if ( newValue == null ) newValue = IPNetwork.getEmptyNetwork();
        this.destination = newValue;
    }

    /**
     * The IP address of the next router.
     *
     * @return The IP address of the router that should accept the packets.
     *
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="next_hop"
     * sql-type="inet"
     */
    public IPaddr getNextHop()
    {
        if ( this.nextHop == null ) this.nextHop = NetworkUtil.EMPTY_IPADDR;
        return this.nextHop;
    }

    public void setNextHop( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.nextHop = newValue;
    }

    public String toString()
    {
        return "destination: "  + getDestination() + " next-hop: " + getNextHop();
    }
}
