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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.IPaddr;

/**
 * Log event for client distribution.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_openvpn_distr_evt", schema="events")
@SuppressWarnings("serial")
public class ClientDistributionEvent extends LogEvent implements Serializable
{

    private IPaddr address;
    private String clientName;

    // Constructors
    public ClientDistributionEvent() { }

    public ClientDistributionEvent( IPaddr address, String clientName )
    {
        this.address    = address;
        this.clientName = clientName;
    }

    /**
     * Address of the client that performed the request, null if the client
     * was downloaded directly to a USB key.
     *
     * @return Address of the client that performed the request.
     */
    @Column(name="remote_address")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getAddress()
    {
        return this.address;
    }

    public void setAddress( IPaddr address )
    {
        this.address = address;
    }

    /**
     * Name of the client that was distributed.
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

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("client-addr", address.getAddr());
        sb.addField("client-name", clientName);
    }

    @Transient
    public String getSyslogId()
    {
        return "Client_Distribution";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }
}
