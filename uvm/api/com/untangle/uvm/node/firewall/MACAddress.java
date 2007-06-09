
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

package com.untangle.uvm.node.firewall;

import java.io.Serializable;

import com.untangle.uvm.node.ParseException;

/**
 * A class to hold a MAC address.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class MACAddress implements Serializable, Comparable
{
    /* Number of hex digits in a MAC address */
    private static final int MACADDRSZ = 6;

    /* A sample MAC address  */
    private static final String SAMPLE = "01:23:45:67:89:ab";

    /* String representation of the MAC address */
    private final String mac;

    private MACAddress( String mac )
    {
        this.mac = mac.toLowerCase();
    }

    /**
     * Parse a string and create a new MACAddress object.
     *
     * @param mac String value to parse.
     */
    public static MACAddress parse( String mac ) throws ParseException
    {
        /* Trim any whitespace */
        mac = mac.trim();

        /* Use seven to guarantee it doesn't converted  x:x:x:x:x:x:x to { x, x, x, x, x, x:x } */
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

    /**
     * Retrieve a sample String for a MAC Address.
     */
    public static String sample()
    {
        return SAMPLE;
    }

    public int hashCode()
    {
        /* The value here is just the MAC which is a string, so pass these down to the string */
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

    public int compareTo( Object o )
    {
        return mac.compareToIgnoreCase(o.toString());
    }
}
