/*
 * $Id$
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
    private final String hostName;
    
    /* the IP represented by this host address, can be null. */
    private final IPAddress ip;

    public HostAddress( IPAddress ip )
    {
        this( ip, null );
    }

    public HostAddress( String hostName )
    {
        this( null, hostName );
    }

    public HostAddress( IPAddress ip, String hostName )
    {
        this.ip = ip;
        this.hostName = hostName;
    }

    public String getHostName()
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
        String hostName = null;

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
            hostName = address;
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
}