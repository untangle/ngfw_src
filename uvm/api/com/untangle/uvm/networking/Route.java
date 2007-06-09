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

package com.untangle.uvm.networking;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Rule;
import org.hibernate.annotations.Type;

/**
 * A routing entry
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="uvm_network_route", schema="settings")
public class Route extends Rule
{
    /**
     * unused.
     * Presently the network space is only used to display routes that
     * are immutable.
     */
    private NetworkSpace networkSpace = null;

    /* The network for this route. */
    private IPNetwork destination;
    
    /* The next hop that should be used for this route */
    private IPaddr nextHop;

    public Route() { }

    public Route( NetworkSpace networkSpace, IPNetwork destination,
                  IPaddr nextHop )
    {
        this.networkSpace = networkSpace;
        this.destination  = destination;
        this.nextHop      = nextHop;
    }

    /**
     * unused
     * Get the network space associated with this route.
     *
     * @return The network space this route belongs to, this will not
     * be supported until =RELEASE3.3=.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="network_space")
    public NetworkSpace getNetworkSpace()
    {
        return this.networkSpace;
    }

    /**
     * unused
     * Set the network space associated with this route.
     *
     * @param networkSpace The network space that should be 
     * associated with this route.
     */
    public void setNetworkSpace( NetworkSpace networkSpace )
    {
        this.networkSpace = networkSpace;
    }

    /**
     * Destination network that triggers this routing entry.
     *
     * @return The destination network this route is related to.
     */
    @Type(type="com.untangle.uvm.networking.IPNetworkUserType")
    public IPNetwork getDestination()
    {
        if ( this.destination == null ) {
            this.destination = IPNetwork.getEmptyNetwork();
        }
        return this.destination;
    }

    /**
     * Set the destination network that triggers this routing entry.
     *
     * @param newValue The destination network this route is related
     * to.
     */
    public void setDestination( IPNetwork newValue )
    {
        if ( newValue == null ) newValue = IPNetwork.getEmptyNetwork();
        this.destination = newValue;
    }

    /**
     * The IP address of the next router.
     *
     * @return The IP address of the router that can route traffic
     * destined to <code>destination</code>.
     */
    @Column(name="next_hop")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getNextHop()
    {
        if ( this.nextHop == null ) this.nextHop = NetworkUtil.EMPTY_IPADDR;
        return this.nextHop;
    }

    /**
     * Set the IP address of the next router.
     *
     * @param newValue The IP address of the router that can route
     * traffic destined to <code>destination</code>.
     */
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
