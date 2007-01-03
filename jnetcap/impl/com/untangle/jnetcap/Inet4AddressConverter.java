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

package com.untangle.jnetcap;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;

public class Inet4AddressConverter {   
    static final int INADDRSZ = 4;
    
    /* No point in creating any instances */
    private Inet4AddressConverter() {
    }

    public static InetAddress getByAddress ( String dotNotation )
    {
        int input[] = new int[INADDRSZ];

        /* Trim any whitespace */
        dotNotation = dotNotation.trim();
        
        /* Use five to guarantee it doesn't converted from x.x.x.x.x to { x, x, x, x.x } */
        String tmp[] = dotNotation.split( "\\.", INADDRSZ + 1 );

        if ( tmp.length != INADDRSZ ) {
            Netcap.logError( "UnknownHostException: Invalid dot notation - " + dotNotation );
            return null;
        }

        for ( int c = 0 ; c < tmp.length ; c++ ) {
            input[c] = Integer.parseInt( tmp[c] );
            if ( input[c] < 0 || input[c] > 255 ) {
                Netcap.logError( "UnknownHostException: Invalid dot notation - " + dotNotation );
                return null;
            }
        }

        return getByAddress( input );
    }

    public static InetAddress getByHexAddress( String hex, boolean isLittleEndian ) {
        int input[] = new int[INADDRSZ];
        
        hex = hex.trim();
        
        for( int c = 0 ; c < INADDRSZ ; c++ ) {
            int tmp = Integer.parseInt( hex.substring( c*2, c*2 + 2), 16 );
            if ( isLittleEndian ) {
                input[(INADDRSZ - 1) - c] = tmp;
            } else {
                input[c] = tmp;
            }
        }
        
        return getByAddress( input );

    }

    public static InetAddress getByAddress ( int input[] ) {
        byte byteArray[] = new byte[INADDRSZ];
        InetAddress address = null;

        if ( input.length != INADDRSZ ) {
            Netcap.error( "Invalid input length" );
            return null;
        }
        
        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            byteArray[c] = (byte)input[c];;
        }
        
        try {
            address = Inet4Address.getByAddress( byteArray );
        } catch ( UnknownHostException e ) {
            /* ??? This should never happen */
            Netcap.logError( "UnknownHostException: " + e.getMessage());
        }
        
        return address;
    }


    public static long toLong ( InetAddress address )
    {
        long val = 0;
        int c;
        
        byte valArray[] = address.getAddress();
        
        for ( c = 0 ; c < INADDRSZ ; c++ ) {
            val += ((long)byteToInt(valArray[c])) << ( 8 * c );
        }

        return val;
    }

    public static InetAddress toAddress ( long val ) 
    {
        byte valArray[] = new byte[INADDRSZ];
        InetAddress address = null;
                
        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            valArray[c] = (byte)((val >> ( 8 * c)) & 0xFF);
        }
        
        try {
            address = Inet4Address.getByAddress ( valArray );
        } catch ( UnknownHostException e ) {
            /* ??? This should never happen */
            Netcap.logError( "UnknownHostException: " + e.getMessage());
        }

        return address;
    }

    private static int byteToInt ( byte val ) {
        int num = val;
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }
}
