/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.router;

import java.util.Date;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.MACAddress;
import org.apache.log4j.Logger;

/* XXX Probably should be an inner class for DhcpMonitor */
public class DhcpLease
{
    private static final int EXPIRED = 0;
    private static final int ACTIVE  = 1;

    private final Logger logger = Logger.getLogger(getClass());

    private MACAddress mac        = null;
    private HostName   hostname   = HostName.getEmptyHostName();
    private IPAddress     ip         = null;
    private Date       endOfLease = null;
    private int        state      = EXPIRED;

    // Constructors
    /**
     * Hibernate constructor
     */
    public DhcpLease()
    {
    }

    public DhcpLease( Date endOfLease, MACAddress mac, IPAddress ip, HostName hostname, Date now )
    {
        this.endOfLease = endOfLease;
        this.mac        = mac;
        this.ip         = ip;
        this.hostname   = hostname;
        updateState( now );
    }

    /**
     * @return true if the passed in parameters are different from the current parameters
     */
    boolean hasChanged( Date endOfLease, MACAddress mac, IPAddress ip, HostName hostname, Date now )
    {
        int state = this.state;
        updateState( now );

        /**
         * A DhcpLease is suppose to track the lease on a specific IP
         */
        if ( !this.ip.equals( ip )) {
            logger.warn( "hasChanged with different ip: " + this.ip.toString() + " ->" + ip.toString());
            return true;
        }

        if ( this.state != state || !this.endOfLease.equals( endOfLease ) || !this.mac.equals( mac ) ||
             !this.hostname.equals( hostname )) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if these new values represent a lease renewal
     */
    boolean isRenewal( MACAddress mac, HostName hostname )
    {
        /* renewal if the previous lease was active, and mac and hostname have not changed */
        return isActive() && this.mac.equals( mac ) && this.hostname.equals( hostname );
    }

    /**
     * @return true if the lease was active when this object was created or last updated.
     */
    boolean isActive()
    {
        return ( state == ACTIVE ) ? true : false;
    }

    void set( Date endOfLease, MACAddress mac, IPAddress ip, HostName hostname, Date now )
    {
        this.endOfLease = endOfLease;
        this.mac        = mac;
        this.ip         = ip;
        this.hostname   = hostname;
        updateState( now );
    }

    void updateState( Date now )
    {
        this.state = ( now.before( endOfLease )) ? ACTIVE : EXPIRED;
    }

    public MACAddress getMac()
    {
        return mac;
    }

    public void setMac( MACAddress mac )
    {
        this.mac = mac;
    }

    public HostName getHostname()
    {
        return hostname;
    }

    public void setHostname( HostName hostname )
    {
        this.hostname = hostname;
    }

    public IPAddress getIP()
    {
        return this.ip;
    }

    public void setIP( IPAddress ip )
    {
        this.ip = ip;
    }

    public Date getEndOfLease()
    {
        return endOfLease;
    }

    public void setEndOfLease( Date endOfLease )
    {
        this.endOfLease = endOfLease;
    }
}
