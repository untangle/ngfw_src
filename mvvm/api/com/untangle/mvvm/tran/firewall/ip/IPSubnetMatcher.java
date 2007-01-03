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

package com.untangle.mvvm.tran.firewall.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;

import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class IPSubnetMatcher extends IPDBMatcher
{
    private static final String MARKER_SUBNET = "/";

    private final long network, netmask;
    private final String string;

    private IPSubnetMatcher( long network, long netmask, String string )
    {
        this.network = network;
        this.netmask = netmask;
        this.string  = string;
    }

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

    public static IPDBMatcher makeInstance( IPaddr network, IPaddr netmask )
    {
        return makeInstance( network.getAddr(), netmask.getAddr());
    }

    public static IPDBMatcher makeInstance( InetAddress network, InetAddress netmask ) 
    {
        if ( network == null || netmask == null ) throw new NullPointerException( "Null address" );

        IPMatcherUtil imu = IPMatcherUtil.getInstance();
        
        String user = network.getHostAddress() + " " + MARKER_SUBNET +  " " + netmask.getHostAddress();

        long netmaskLong = imu.toLong( netmask );
        long networkLong = imu.toLong( network ) & netmaskLong;
    
        return new IPSubnetMatcher( networkLong, netmaskLong, user );
    }

    public static IPDBMatcher makeInstance( InetAddress network, int cidr )
        throws ParseException
    {
        IPMatcherUtil imu = IPMatcherUtil.getInstance();
        
        String user = network.getHostAddress() + " " + MARKER_SUBNET + " " + cidr;

        return new IPSubnetMatcher( imu.toLong( network ), imu.cidrToLong( cidr ), user );
    }

    /* This is just for matching a list of interfaces */
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

