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

/**
 * Log event for a DHCP lease event.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="DHCP_ABS_LEASE"
 * mutable="false"
 */
public class DhcpAbsoluteLease 
{
    static final int REGISTERED = 0;
    static final int EXPIRED    = 1;

    private Long id;
    private MACAddress mac;
    private HostName   hostname;
    private IPaddr     ip;
    private Date       endOfLease;
    private int        eventType;

    // Constructors 
    /**
     * Hibernate constructor 
     */
    public DhcpAbsoluteLease()
    {
    }

    /**
     * XXX Event type should be an enumeration or something */
    public DhcpAbsoluteLease( DhcpLease lease, Date now  )
    {
        this.endOfLease = lease.getEndOfLease();
        this.mac        = lease.getMac();
        this.ip         = lease.getIP();
        this.hostname   = lease.getHostname();
        this.eventType  = now.after( endOfLease ) ? EXPIRED : REGISTERED;
    }

    /**
     * @hibernate.id
     * column="EVENT_ID"
     * generator-class="native"
     */
    protected Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }
    
    /**
     * MAC address
     *
     * @return the mac address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.MACAddressUserType"
     * @hibernate.column
     * name="MAC"
     */    
    public MACAddress getMac()
    {
        return mac;
    }

    public void setMac( MACAddress mac )
    {
        this.mac = mac;
    }
    
    /**
     * Host name
     *
     * @return the host name.
     * @hibernate.property
     * type="com.metavize.mvvm.type.HostNameUserType"
     * @hibernate.column
     * name="HOSTNAME"
     */    
    public HostName getHostname()
    {
        return hostname;
    }

    public void setHostname( HostName hostname )
    {
        this.hostname = hostname;
    }

    /**
     * Get IP address for this lease
     *
     * @return desired static address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="IP"
     * sql-type="inet"
     */
    public IPaddr getIP()
    {
        return this.ip;
    }
    
    public void setIP( IPaddr ip ) 
    {
        this.ip = ip;
    }
    
    /**
     * Expiration date of the lease.
     *
     * @return expiration date.
     * @hibernate.property
     * column="END_OF_LEASE"
     */
    public Date getEndOfLease()
    {
        return endOfLease;
    }

    public void setEndOfLease( Date endOfLease )
    {
        this.endOfLease = endOfLease;
    }


    /**
     * State of the lease.
     *
     * @return expiration date.
     * @hibernate.property
     * column="EVENT_TYPE"
     */
    public int getEventType()
    {
        return eventType;
    }

    public void setEventType( int eventType )
    {
        this.eventType = eventType;
    }

}
