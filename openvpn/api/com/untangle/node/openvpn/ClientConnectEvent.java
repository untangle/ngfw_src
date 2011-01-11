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

package com.untangle.node.openvpn;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.IPAddress;

/**
 * Log event for when a client logs in.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_openvpn_connect_evt", schema="events")
@SuppressWarnings("serial")

public class ClientConnectEvent extends LogEvent implements Serializable
{

    private IPAddress address;
    private int port;
    private String clientName;
    private Date start; /* Start of the session */
    private Date end; /* End of the session */
    private long bytesRx; /* Total bytes received */
    private long bytesTx; /* Total bytes transmitted */

    // Constructors
    public ClientConnectEvent() {}

    public ClientConnectEvent( Date start, IPAddress address, int port, String clientName )
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
     */
    @Column(name="remote_address")
    @Type(type="com.untangle.uvm.type.IPAddressUserType")
    public IPAddress getAddress()
    {
        return this.address;
    }

    public void setAddress( IPAddress address )
    {
        this.address = address;
    }

    /**
     * Port used to connect
     *
     * @return Client port
     */
    @Column(name="remote_port", nullable=false)
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
     */
    @Column(name="client_name")
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
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="start_time")
    public Date getStart()
    {
        return this.start;
    }

    void setStart( Date start )
    {
        if( start instanceof Timestamp )
            this.start = new Date(start.getTime());
        else
            this.start = start;
    }

    /**
     * Time the session ended. <b>Note that this
     * may be null if the session is still open</b>
     *
     * @return time logged.
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="end_time")
    public Date getEnd()
    {
        return this.end;
    }

    void setEnd( Date end )
    {
        if( end instanceof Timestamp ) {
            this.end = new Date(end.getTime());
        } else {
            this.end = end;
        }
    }

    /**
     * Total bytes received during this session.
     *
     * @return time logged.
     */
    @Column(name="rx_bytes", nullable=false)
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
     */
    @Column(name="tx_bytes", nullable=false)
    public long getBytesTx()
    {
        return this.bytesTx;
    }

    void setBytesTx( long bytesTx )
    {
        this.bytesTx = bytesTx;
    }

    // Syslog methods ---------------------------------------------------------

    // although events are created with only some data (see constructor),
    // all data will be set when events are actually and finally logged
    // (gui event log receives snapshot copies of these events without end time)
    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("start", getStart());
        sb.addField("end", getEnd());
        sb.addField("client-name", getClientName());
        sb.addField("client-addr", getAddress().getAddr());
        sb.addField("client-port", getPort());
        sb.addField("bytes-received", getBytesRx());
        sb.addField("bytes-transmitted", getBytesTx());
    }

    @Transient
    public String getSyslogId()
    {
        return "Client_Connect";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }
}
