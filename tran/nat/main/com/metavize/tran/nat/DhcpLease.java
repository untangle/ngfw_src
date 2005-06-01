/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import java.util.Date;

import com.metavize.mvvm.tran.Rule;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.firewall.MACAddress;

public class DhcpLease 
{
    private static final int EXPIRED = 0;
    private static final int ACTIVE  = 1;

    private MACAddress mac        = null;
    private HostName   hostname   = HostName.getEmptyHostName();
    private IPaddr     ip         = null;
    private Date       endOfLease = null;
    private int        state      = EXPIRED;

    // Constructors 
    /**
     * Hibernate constructor 
     */
    public DhcpLease()
    {
    }

    public DhcpLease( Date endOfLease, MACAddress mac, IPaddr ip, HostName hostname, Date now )
    {
        this.endOfLease = endOfLease;
        this.mac        = mac;
        this.ip         = ip;
        this.hostname   = hostname;
        updateState( now );
    }

    boolean hasChanged( Date endOfLease, MACAddress mac, IPaddr ip, HostName hostname, Date now )
    {
        int state = this.state;
        updateState( now );
        
        if ( this.state == state && this.endOfLease.equals( endOfLease ) && this.mac.equals( mac ) && 
             this.ip.equals( ip ) && this.hostname.equals( hostname )) {
            return true;
        }
        
        return false;
    }
    
    void set( Date endOfLease, MACAddress mac, IPaddr ip, HostName hostname, Date now )
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

    public IPaddr getIP()
    {
        return this.ip;
    }
    
    public void setIP( IPaddr ip ) 
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
