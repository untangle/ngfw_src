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

package com.untangle.uvm.node.firewall.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;

/**
 * An IPMatcher that matches a single subnet.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public final class IPSubnetMatcher extends IPDBMatcher
{
    /* The marker that separates the network from the netmask */
    private static final String MARKER_SUBNET = "/";

    /* The network represented as a long */
    private final long network;

    /* The netmask represented as a long */
    private final long netmask;

    /* The database string for this matcher */
    private final String string;

    private IPSubnetMatcher( long network, long netmask, String string )
    {
        this.network = network;
        this.netmask = netmask;
        this.string  = string;
    }

    /**
     * Test if <param>address<param> matches this matcher.
     *
     * @param address The address to test.
     * @return True if <param>address</param> matches is in the subnet
     * this matcher describes.
     */
    public boolean isMatch( InetAddress address )
    {
        long tmp = IPMatcherUtil.getInstance().toLong( address );

        return (( tmp & netmask ) == network );
    }

    public String toDatabaseString()
    {
        return toString();
    }

    public String toString()
    {
        return this.string;
    }

    /**
     * Create a new matcher that matches the subnet
     * <param>network<param>/<param>netmask</param>
     *
     * @param network The network address to match.
     * @param netmask The netmask/size of <param>network</param>.
     * @return An IPMatcher that matches the specified subnet.
     */
    public static IPDBMatcher makeInstance( IPAddress network, IPAddress netmask )
    {
        return makeInstance( network.getAddr(), netmask.getAddr());
    }

    /**
     * Create a new matcher that matches the subnet
     * <param>network<param>/<param>netmask</param>.
     *
     * @param network The network address to match.
     * @param netmask The netmask/size of <param>network</param>.
     * @return An IPMatcher that matches the specified subnet.
     */
    public static IPDBMatcher makeInstance( InetAddress network, InetAddress netmask )
    {
        if ( network == null || netmask == null ) throw new NullPointerException( "Null address" );

        IPMatcherUtil imu = IPMatcherUtil.getInstance();

        String user = network.getHostAddress() + " " + MARKER_SUBNET +  " " + netmask.getHostAddress();

        long netmaskLong = imu.toLong( netmask );
        long networkLong = imu.toLong( network ) & netmaskLong;

        return new IPSubnetMatcher( networkLong, netmaskLong, user );
    }

    /**
     * Create a new matcher that matches the subnet
     * <param>network<param>/<param>cidr</param>.
     *
     * @param network The network address to match.
     * @param cidr The netmask in CIDR notation.
     * @return An IPMatcher that matches the specified subnet.
     */
    public static IPDBMatcher makeInstance( InetAddress network, int cidr )
        throws ParseException
    {
        IPMatcherUtil imu = IPMatcherUtil.getInstance();

        String user = network.getHostAddress() + " " + MARKER_SUBNET + " " + cidr;
        long netmaskLong = imu.cidrToLong( cidr );
        long networkLong = imu.toLong( network ) & netmaskLong;
        
        return new IPSubnetMatcher( networkLong, netmaskLong, user );
    }

    /* The parser for an Subnet Matcher */
    static final Parser<IPDBMatcher> PARSER = new Parser<IPDBMatcher>()
    {
        public int priority()
        {
            return 10;
        }

        public boolean isParseable( String value )
        {
            return ( value.contains( MARKER_SUBNET ));
        }

        public IPDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid ip subnet matcher '" + value + "'" );
            }

            String ipArray[] = value.split( MARKER_SUBNET );
            if ( ipArray.length != 2 ) {
                throw new ParseException( "Subnet matcher contains two components: " + value );
            }

            try {
                String cidr = ipArray[1].trim();
                if ( cidr.length() < 3 ) {
                    try {
                        return makeInstance( IPAddress.parse( ipArray[0] ).getAddr(),
                                             Integer.parseInt( cidr ));
                    } catch ( NumberFormatException e ) {
                        throw new ParseException( "Invalid CIDR notion: '" + cidr +
                                                  "' should be a number between 0 and 32" );
                    }
                } else {
                    return makeInstance( IPAddress.parse( ipArray[0] ).getAddr(),
                                         IPAddress.parse( ipArray[1] ).getAddr());
                }
            } catch ( UnknownHostException e ) {
                throw new ParseException( e );
            }
        }
    };
}

