/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import java.io.Serializable;

import java.net.UnknownHostException;

import com.metavize.mvvm.tran.IPaddr;

import com.metavize.mvvm.tran.ParseException;

public class IPNetwork implements Serializable
{
    private static final String MARKER_SUBNET = "/";
    
    private final IPaddr network;
    private final IPaddr subnet;
    private final String user;
    
    /* XXX Perhaps this should be stored in CIDR notation */
    private IPNetwork( IPaddr network, IPaddr subnet, String user )
    {
        this.network = network;
        this.subnet = subnet;
        this.user = user;
    }

    public IPaddr getNetwork()
    {
        return this.network;
    }

    public IPaddr getSubnet()
    {
        return this.subnet;
    }

    public String toString()
    {
        return this.user;
    }

    public static IPNetwork parse( String value ) throws ParseException
    {
        value = value.trim();

        String ipArray[] = value.split( MARKER_SUBNET );
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
}
