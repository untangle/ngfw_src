
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

package com.untangle.uvm.node.firewall;

import java.io.Serializable;

import com.untangle.uvm.node.ParseException;

/**
 * A class to hold a MAC address.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public final class MACAddress implements Serializable, Comparable<MACAddress>
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

    public int compareTo( MACAddress m )
    {
        return mac.compareToIgnoreCase(m.toString());
    }
}
