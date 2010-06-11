/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.networking;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Rule;

/**
 * A routing entry
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_network_route", schema="settings")
@SuppressWarnings("serial")
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
