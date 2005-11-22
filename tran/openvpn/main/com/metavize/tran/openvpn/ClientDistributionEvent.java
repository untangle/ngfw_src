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
 * Log event for client distribution.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_openvpn_distr_evt"
 * mutable="false"
 */
public class ClientDistributionEvent extends LogEvent implements Serializable
{
    private IPaddr address;
    private String clientName;

    // Constructors
    /**
     * Hibernate constructor
     */
    public ClientDistributionEvent()
    {
    }

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
     * Name of the client that was distributed.
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

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("client-address", address.getAddr());
        sb.addField("client-name", clientName);
    }
}
