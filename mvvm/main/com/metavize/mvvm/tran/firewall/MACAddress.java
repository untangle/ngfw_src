
/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: IPaddr.java 400 2005-04-22 21:55:25Z rbscott $
 */

package com.metavize.mvvm.tran.firewall;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MACAddress implements Serializable
{
    static final int MACADDRSZ = 6;
    static final String SAMPLE = "01:23:45:67:89:AB";
    
    private final String mac;

    private MACAddress( String mac )
    {
        this.mac = mac;
    }
    
    public static MACAddress parse( String mac ) throws IllegalArgumentException, UnknownHostException
    {
        /* Trim any whitespace */
        mac = mac.trim();

        /* Use five to guarantee it doesn't converted from x.x.x.x.x to { x, x, x, x.x } */
        String tmp[] = mac.split( ":", MACADDRSZ + 1 );

        if ( tmp.length != MACADDRSZ ) {
            throw new IllegalArgumentException( "Invalid MAC Address " + mac );
        }

        /* Validation */
        for ( int c = 0 ; c < tmp.length ; c++ ) {
            int val = Integer.parseInt( tmp[c], 16 );
            if ( val < 0 || val > 255 ) {
                throw new IllegalArgumentException( "Each component must be between 0 and 255 " + tmp[c] );
            }
        }

        return new MACAddress( mac );
    }

    public String toString()
    {
        /* Return a copy of the MAC address */
        return new String( mac );
    }

    public static String sample()
    {
        return SAMPLE;
    }
}
