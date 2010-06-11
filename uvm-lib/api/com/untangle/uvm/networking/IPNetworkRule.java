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

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.firewall.ParsingConstants;



/**
 * An IPNetwork that is to go into a list, this is only to
 * allow lists of IPNetworks to be saved.  Normally, an IPNetwork is
 * just stored using the IPNetworkUserType.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_ip_network", schema="settings")
public class IPNetworkRule extends Rule
{

    /** The IP Network for this rule */
    private IPNetwork ipNetwork;

    public IPNetworkRule() { }

    public IPNetworkRule( IPNetwork ipNetwork )
    {
        this.ipNetwork = ipNetwork;
    }

    /**
     * The IPNetwork associated with this rule.
     * @return The IPNetwork associated with this rule.
     */
    @Column(name="network")
    @Type(type="com.untangle.uvm.networking.IPNetworkUserType")
    public IPNetwork getIPNetwork()
    {
        return this.ipNetwork;
    }

    /**
     * Set the IP Network associated with this rule.
     * @param newValue The IPNetwork associated with this rule.
     */
    public void setIPNetwork( IPNetwork newValue )
    {
        this.ipNetwork = newValue;
    }

    /* The following are convenience methods, an IPNetwork is
     * immutable, so the corresponding setters do not exist */

    /**
     * Retrieve the network associated with this IP Network
     *
     * @return The network associated with this IP Network.
     */
    @Transient
    public IPaddr getNetwork()
    {
        return this.ipNetwork.getNetwork();
    }

    /**
     * Retrieve the netmask associated with this IP Network
     *
     * @return The netmask associated with this IP Network.
     */
    @Transient
    public IPaddr getNetmask()
    {
        return this.ipNetwork.getNetmask();
    }

    /**
     * True if <code>network</code> is a unicast address.
     *
     * @return  True iff <code>network</code> is a unicast address.
     */
    @Transient
    public boolean isUnicast()
    {
        return this.ipNetwork.isUnicast();
    }

    public String toString()
    {
        if ( this.ipNetwork == null ) return "null";
        else return this.ipNetwork.toString();
    }

    /**
     * Parse a single IPNetwork and create a new
     * <code>IPNetworkRule</code>.  An IPNetwork is formmatted as
     * x.y.z.w / 0-32 or x.y.z.w / a.b.c.d.
     * 
     * @param value The value to parse.
     * @return A new IP Network based on <code>value</code>.
     * @exception ParseException If <code>value</code> isn't a
     * properly formatted IPNetwork.
     */
    public static IPNetworkRule parse( String value ) throws ParseException
    {
        return new IPNetworkRule( IPNetwork.parse( value ));
    }

    /**
     * Parse a list of IPNetworks and return a new list containing
     * each of the values.
     *
     * @param value The list of IPNetworks each separated by a
     * <code>ParsingConstants.MARKER_SEPERATOR</code>.
     * @return A new list of IPNetworks corresponding the the values
     * in <code>value</code>
     * @exception ParseException if any of the values are not properly formatted.
     */
    public static List<IPNetworkRule> parseList( String value ) throws ParseException
    {
        List<IPNetworkRule> networkList = new LinkedList<IPNetworkRule>();

        /* empty list, null or throw parse exception */
        if ( value == null ) throw new ParseException( "null list" );

        value = value.trim();

        String networkArray[] = value.split( ParsingConstants.MARKER_SEPERATOR );

        for ( int c = 0 ; c < networkArray.length ; c++ ) networkList.add( parse( networkArray[c] ));
        return networkList;
    }

    /**
     * Create a new IP Network Rule.
     *
     * @param network The network.
     * @param netmask The netmask of this network.
     * @return A new IP Network Rule for <code>network / netmask</code>
     */
    public static IPNetworkRule makeInstance( InetAddress network, InetAddress netmask )
    {
        return new IPNetworkRule( IPNetwork.makeInstance( network, netmask ));
    }

    /**
     * Create a new IP Network Rule.
     *
     * @param network The network.
     * @param netmask The netmask of this network.
     * @return A new IP Network Rule for <code>network / netmask</code>
     */
    public static IPNetworkRule makeInstance( IPaddr network, IPaddr netmask )
    {
        return new IPNetworkRule( IPNetwork.makeInstance( network, netmask ));
    }
}
