/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.openvpn;

import java.io.Serializable;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;

import com.metavize.mvvm.tran.IPaddr;

/**
 * Log event for when a client logs in.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_openvpn_connect_evt"
 * mutable="false"
 */
public class ClientConnectEvent extends LogEvent implements Serializable
{
    private IPaddr address;
    private int    port;

    private String clientName;
    
    // Constructors 
    /**
     * Hibernate constructor 
     */
    public ClientConnectEvent()
    {
    }

    public ClientConnectEvent( IPaddr address, int port, String clientName )
    {
        this.address    = address;
        this.clientName = clientName;
        this.port       = port;
    }

    /**
     * Address where the client connected from.
     *
     * @return Address of where the client connected from.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="remote_address"
     * sql-type="inet"
     */
    public IPaddr getAddress()
    {
        return this.address;
    }
    
    public void setAddress( IPaddr address )
    {
        this.address = address;
    }

    /**
     * Port used to connect
     *
     * @return Client port
     * @hibernate.property
     * column="remote_port"
     */
    public int getPort()
    {
        return this.port;
    }

    public void setPort( int port )
    {
        this.port = port;
    }

    /**
     * Name of the client that was connected.
     *
     * @return Client name
     * @hibernate.property
     * column="client_name"
     */
    public String getClientName()
    {
        return this.clientName;
    }

    public void setClientName( String clientName )
    {
        this.clientName = clientName;
    }

    public void appendSyslog(SyslogBuilder a)
    {
        /* XXXXXXX */
    }

}
