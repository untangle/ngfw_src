/*
 * Copyright (c) 2003-2006 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.networking;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.Rule;
import org.hibernate.annotations.Type;

/**
 * A routing entry
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_network_route", schema="settings")
public class Route extends Rule
{
    /**
     * Presently the network space is only used to display routes that
     * are immutable
     */
    private NetworkSpace networkSpace = null;

    private IPNetwork destination;
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
     * @return The network space this route belongs to, this will not
     * be supported until =RELEASE3.3=.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="network_space")
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
     */
    @Type(type="com.untangle.mvvm.networking.IPNetworkUserType")
    public IPNetwork getDestination()
    {
        if ( this.destination == null ) {
            this.destination = IPNetwork.getEmptyNetwork();
        }
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
     */
    @Column(name="next_hop")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
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
