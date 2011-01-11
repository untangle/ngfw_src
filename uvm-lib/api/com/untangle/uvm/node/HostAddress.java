
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

package com.untangle.uvm.node;

import java.io.Serializable;
import java.net.UnknownHostException;

/** Class to represent the address of a host, this can either be a
 * hostname, an ip or a hostname and an ip.  Rarely should both be
 * set, but in some cases this can be useful. */
@SuppressWarnings("serial")
public class HostAddress implements Serializable
{
    /* either the hostname or the ip must be null */

    /* hostname can be null, at which point the hostname is actually the ip */
    private final HostName hostName;
    
    /* the IP represented by this host address, can be null. */
    private final IPAddress ip;

    public HostAddress( IPAddress ip )
    {
        this( ip, null );
    }

    public HostAddress( HostName hostName )
    {
        this( null, hostName );
    }

    public HostAddress( IPAddress ip, HostName hostName )
    {
        this.ip = ip;
        this.hostName = hostName;
    }

    public HostName getHostName()
    {
        return this.hostName;
    }

    public IPAddress getIp()
    {
        return this.ip;
    }

    public String toString()
    {
        /* lean towards the hostname */
        if ( this.hostName != null && !this.hostName.isEmpty()) return this.hostName.toString();

        return (this.ip==null)?null:this.ip.toString();
    }
    
    /* Address is either a hostname or an ip address */
    public static HostAddress parse( String address ) throws ParseException
    {
        IPAddress ip = null;
        HostName hostName = null;

        try {
            ip = IPAddress.parse( address );
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