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

package com.untangle.uvm.node.firewall.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;

/**
 * An IPMatcher that matches a single subnet.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
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
    public static IPDBMatcher makeInstance( IPaddr network, IPaddr netmask )
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

        return new IPSubnetMatcher( imu.toLong( network ), imu.cidrToLong( cidr ), user );
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
                        return makeInstance( IPaddr.parse( ipArray[0] ).getAddr(),
                                             Integer.parseInt( cidr ));
                    } catch ( NumberFormatException e ) {
                        throw new ParseException( "Invalid CIDR notion: '" + cidr +
                                                  "' should be a number between 0 and 32" );
                    }
                } else {
                    return makeInstance( IPaddr.parse( ipArray[0] ).getAddr(),
                                         IPaddr.parse( ipArray[1] ).getAddr());
                }
            } catch ( UnknownHostException e ) {
                throw new ParseException( e );
            }
        }
    };
}

