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
import java.util.Date;

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
    private static final long serialVersionUID = 5000360330098789417L;
    
    private IPaddr address;
    private int    port;

    private String clientName;

    /* Start of the session */
    private Date start;
    
    /* End of the session */
    private Date end;
    
    /* Total bytes received */
    private long bytesRx;
    
    /* Total bytes transmitted */
    private long bytesTx;
    
    // Constructors 
    /**
     * Hibernate constructor 
     */
    public ClientConnectEvent()
    {
    }

    public ClientConnectEvent( Date start, IPaddr address, int port, String clientName )
    {
        this.start      = start;
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

    /**
     * Time the session started.
     *
     * @return time logged.
     * @hibernate.property
     * column="start_time"
     */
    public Date getStart()
    {
        return this.start;
    }
    
    void setStart( Date start )
    {
        this.start = start;
    }

    /**
     * Time the session ended.
     *
     * @return time logged.
     * @hibernate.property
     * column="end_time"
     */
    public Date getEnd()
    {
        return this.end;
    }

    void setEnd( Date end )
    {
        this.end = end;
    }

    /**
     * Total bytes received during this session.
     *
     * @return time logged.
     * @hibernate.property
     * column="rx_bytes"
     */
    public long getBytesRx()
    {
        return this.bytesRx;
    }

    void setBytesRx( long bytesRx )
    {
        this.bytesRx = bytesRx;
    }
    
    /**
     * Total transmitted received during this session.
     *
     * @return time logged.
     * @hibernate.property
     * column="tx_bytes"
     */
    public long getBytesTx()
    {
        return this.bytesTx;
    }

    void setBytesTx( long bytesTx )
    {
        this.bytesTx = bytesTx;
    }

    public void appendSyslog( SyslogBuilder sb )
    {
        /* XXXXXXX */
    }
}
