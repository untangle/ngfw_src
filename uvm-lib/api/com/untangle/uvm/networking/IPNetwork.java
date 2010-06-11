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

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;

/**
 * A single ip network.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class IPNetwork implements Serializable
{

    /* The separator between the network and the netmask */
    private static final String MARKER_NETMASK = "/";
    
    /* An empty IP network, use this to initialize IPNetwork when a
     * default value is needed. */
    private static final IPNetwork EMPTY_IPNETWORK;
    
    /* The network */
    private final IPaddr network;

    /* The netmask */
    private final IPaddr netmask;

    /* User/Database representation for this IPNetwork. */
    private final String user;

    /* XXX Perhaps this should be stored in CIDR notation */
    private IPNetwork( IPaddr network, IPaddr netmask, String user )
    {
        this.network = network;
        this.netmask = netmask;
        this.user = user;
    }

    /**
     * Retrieve the network.
     *
     * @return The network.
     */
    public IPaddr getNetwork()
    {
        return this.network;
    }

    /**
     * Retrieve the netmask for this network.
     *
     * @return The netmask.
     */
    public IPaddr getNetmask()
    {
        return this.netmask;
    }

    /**
     * True if <code>network</code> is a unicast address.
     *
     * @return  True iff <code>network</code> is a unicast address.
     */
    public boolean isUnicast()
    {
        return  NetworkUtil.getInstance().isUnicast( this );
    }

    public String toString()
    {
        return this.user;
    }

    public boolean equals( Object o )
    {
        if (!( o instanceof IPNetwork )) return false;

        IPNetwork ip = (IPNetwork)o;
        
        /* All items are null, true */
        if ( ip.network == null && this.network == null &&
             ip.netmask == null && this.netmask == null ) return true;

        /* One item is null and the other is not, return true */
        if ( ip.network == null && this.network != null ) return false;
        if ( ip.netmask == null && this.netmask != null ) return false;

        if ( !ip.network.equals( this.network )) return false;
        if ( !ip.netmask.equals( this.netmask )) return false;
        
        return true;
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + (( this.network == null ) ? 23 : this.network.hashCode());
        result = 37 * result + (( this.netmask == null ) ? 23 : this.netmask.hashCode());
        return result;
    }

    /**
     * Parse a string and convert it to create the corresponding IPNetwork.
     *
     * @param value The string to parser.
     * @return A IPNetwork that corresponds to <code>value</code>
     * @exception ParseException If value doesn't represent an IPNetwork.
     */
    public static IPNetwork parse( String value ) throws ParseException
    {
        value = value.trim();

        String ipArray[] = value.split( MARKER_NETMASK );
        if ( ipArray.length != 2 ) {
            throw new ParseException( "IP Network contains two components: " + value );
        }
        
        try {
            String cidr = ipArray[1].trim();
            if ( cidr.length() < 3 ) {
                return new IPNetwork( IPaddr.parse( ipArray[0] ), IPaddr.cidrToIPaddr( cidr ), value );
            } else {
                return new IPNetwork( IPaddr.parse( ipArray[0] ), IPaddr.parse( ipArray[1] ), value );
            }
        } catch ( UnknownHostException e ) {
            throw new ParseException( e );
        }
    }

    /**
     * Create a new IP Network.
     *
     * @param network The network.
     * @param netmask The netmask of this network.
     * @return A new IP Network for <code>network / netmask</code>
     */
    public static IPNetwork makeInstance( InetAddress network, InetAddress netmask )
    {
        return makeInstance( new IPaddr((Inet4Address)network), new IPaddr((Inet4Address)netmask));
    }

    /**
     * Create a new IP Network.
     *
     * @param network The network.
     * @param netmask The netmask of this network.
     * @return A new IP Network for <code>network / netmask</code>
     */
    public static IPNetwork makeInstance( IPaddr network, IPaddr netmask )
    {
        String user = network + "/" + netmask;
        return new IPNetwork( network, netmask, user );
    }

    /**
     * Retrieve the empty network, this is useful to initialize values.
     *
     * @return an empty ip network.
     */
    public static IPNetwork getEmptyNetwork()
    {
        return EMPTY_IPNETWORK;
    }

    static
    {
        Inet4Address EMPTY;
        try {
            EMPTY = (Inet4Address)InetAddress.getByName( "0.0.0.0" );
        } catch ( Exception e ) {
            System.err.println( "Unable to parse empty IP address, this is bad" );
            EMPTY = null;
        }

        EMPTY_IPNETWORK = new IPNetwork( new IPaddr( EMPTY ), new IPaddr( EMPTY ), "0.0.0.0/0" );
    }
}
