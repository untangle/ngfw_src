
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

package com.untangle.mvvm.tran;

import java.io.Serializable;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Class to represent the address of a host, this can either be a
 * hostname, an ip or a hostname and an ip.  Rarely should both be
 * set, but in some cases this can be useful. */
public class HostAddress implements Serializable
{
    /* either the hostname or the ip must be null */

    /* hostname can be null, at which point the hostname is actually the ip */
    private final HostName hostName;
    
    /* the IP represented by this host address, can be null. */
    private final IPaddr ip;

    public HostAddress( IPaddr ip )
    {
        this( ip, null );
    }

    public HostAddress( HostName hostName )
    {
        this( null, hostName );
    }

    public HostAddress( IPaddr ip, HostName hostName )
    {
        this.ip = ip;
        this.hostName = hostName;
    }

    public HostName getHostName()
    {
        return this.hostName;
    }

    public IPaddr getIp()
    {
        return this.ip;
    }

    public String toString()
    {
        /* lean towards the hostname */
        if ( this.hostName != null && !this.hostName.isEmpty()) return this.hostName.toString();

        return this.ip.toString();
    }
    
    /* Address is either a hostname or an ip address */
    public static HostAddress parse( String address ) throws ParseException
    {
        IPaddr ip = null;
        HostName hostName = null;

        try {
            ip = IPaddr.parse( address );
        } catch ( ParseException e ) {
            /* It is not an IP, try it as a hostname */
            ip = null;
        } catch ( UnknownHostException e ) {
            /* It is not an IP, try it as a hostname */
            ip = null;
        }

        if ( ip == null ) {
            try {
                hostName = HostName.parse( address );
            } catch ( ParseException f ) {
                hostName = null;
            }
        }

        /* Failed both, time to throw a parse exception */
        if ( ip == null && hostName == null ) {
            throw new ParseException( "Invalid Host Address: " + address + 
                                      ".  A Host Address is either a Hostname (www.untangle.com) " + 
                                      "or an IP address (1.2.3.4)." );
        }

        if ( ip != null ) return new HostAddress( ip );        
        return new HostAddress( hostName );
    }

    /* initialize the IP matcher */
    static 
    {
        
    }
    
}