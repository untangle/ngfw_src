
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

package com.untangle.mvvm.tran.firewall;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;


import com.untangle.mvvm.tran.ParseException;

public final class MACAddress implements Serializable, Comparable
{
    static final int MACADDRSZ = 6;
    static final String SAMPLE = "01:23:45:67:89:ab";
    
    private final String mac;

    private MACAddress( String mac )
    {
        this.mac = mac.toLowerCase();
    }
    
    public static MACAddress parse( String mac ) throws ParseException
    {
        /* Trim any whitespace */
        mac = mac.trim();

        /* Use five to guarantee it doesn't converted from x.x.x.x.x to { x, x, x, x.x } */
        String tmp[] = mac.split( ":", MACADDRSZ + 1 );

        if ( tmp.length != MACADDRSZ ) {
            throw new ParseException( "Invalid MAC Address " + mac );
        }

        /* Validation */
        for ( int c = 0 ; c < tmp.length ; c++ ) {
            int val = Integer.parseInt( tmp[c], 16 );
            if ( val < 0 || val > 255 ) {
                throw new ParseException( "Each component must be between 0 and 255 " + tmp[c] );
            }
        }

        return new MACAddress( mac );
    }

    public String toString()
    {
        return mac ;
    }

    public static String sample()
    {
        return SAMPLE;
    }

    /* The value here is just the MAC which is a string, so pass these down to the string */
    public int hashCode()
    {
        /* String is always stored in lowercase so AA:00... would match aa:00 */
        return mac.hashCode();
    }

    public boolean equals( Object o )
    {
        if ( o instanceof MACAddress ) {
            return mac.equalsIgnoreCase(((MACAddress)o).toString());
        }

        return false;
    }

    public int compareTo(Object o){
	return mac.compareToIgnoreCase(o.toString());
    }
}
