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

import com.untangle.mvvm.tran.ParseException;


/**
 * A utility for parsing and dealing with IPv4 addresses.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
class IPMatcherUtil
{
    /* Number of bytes in an IPv4 address */
    static final int INADDRSZ = 4;

    /* The token used to separate the parameters in a range matcher */
    static final String MARKER_RANGE  = "-";

    /* The token used to separate the parameters in a subnet matcher */
    static final String MARKER_SUBNET = "/";

    /* An array of the CIDR values */
    static final String CIDR_STRINGS[] = 
    {
        "0.0.0.0",         "128.0.0.0",       "192.0.0.0",       "224.0.0.0",
        "240.0.0.0",       "248.0.0.0",       "252.0.0.0",       "254.0.0.0",
        "255.0.0.0",       "255.128.0.0",     "255.192.0.0",     "255.224.0.0",
        "255.240.0.0",     "255.248.0.0",     "255.252.0.0",     "255.254.0.0",
        "255.255.0.0",     "255.255.128.0",   "255.255.192.0",   "255.255.224.0",
        "255.255.240.0",   "255.255.248.0",   "255.255.252.0",   "255.255.254.0",
        "255.255.255.0",   "255.255.255.128", "255.255.255.192", "255.255.255.224",
        "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254",
        "255.255.255.255"
    };

    /* Should be an unmodifiable list or vector */
    static final InetAddress CIDR_CONVERTER[] = new InetAddress[CIDR_STRINGS.length];

    private static final IPMatcherUtil INSTANCE = new IPMatcherUtil();
    
    private IPMatcherUtil()
    {
    }

    /**
     * Convert an IPv4 address to a long
     *
     * @param address The address to convert.
     * @return The value of address as a long.
     */
    long toLong( InetAddress address )
    {
        long val = 0;
        
        byte valArray[] = address.getAddress();
        
        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            val += ((long)byteToInt(valArray[c])) << ( 8 * ( INADDRSZ - c - 1 ));
        }
        
        return val;
    }

    /**
     * Convert a CIDR index to an InetAddress.
     *
     * @param cidr CIDR index to convert.
     * @return the InetAddress that corresponds to <param>cidr</param>.
     */
    InetAddress cidrToInetAddress( int cidr ) throws ParseException
    {
        if ( cidr < 0 || cidr > CIDR_CONVERTER.length ) {
            throw new ParseException( "CIDR notation[" + cidr + "] should end with a number between 0 and " + CIDR_CONVERTER.length );
        }

        return CIDR_CONVERTER[cidr];
    }

    /**
     * Convert a CIDR index to a long.
     *
     * @param cidr CIDR index to convert.
     * @return the long that corresponds to <param>cidr</param>.
     */    
    long cidrToLong( int cidr ) throws ParseException
    {
        return toLong( cidrToInetAddress( cidr ));
    }

    /**
     * Convert a CIDR index to a long.
     *
     * @param cidr CIDR index to convert.
     * @return the long that corresponds to <param>cidr</param>.
     */    
    long cidrStringToLong( String cidr ) throws ParseException
    {
        try {
            return cidrToLong( Integer.parseInt( cidr ));
        } catch ( NumberFormatException e ) {
            throw new ParseException( "CIDR Notion[" + cidr + "] should end with a number between 0 and " + CIDR_CONVERTER.length );
        }
    }

    private int byteToInt( byte val )
    {
        int num = val;
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }

    static IPMatcherUtil getInstance()
    {
        return INSTANCE;
    }

    static
    {
        int c = 0;
        for ( String cidr : CIDR_STRINGS ) {
            try {
                CIDR_CONVERTER[c++] = InetAddress.getByName( cidr );
            } catch ( UnknownHostException e ) {
                System.err.println( "Invalid CIDR String at index: " + c );
            }
        }
    }

    
}
