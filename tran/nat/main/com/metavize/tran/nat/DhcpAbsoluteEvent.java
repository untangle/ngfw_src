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

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;

/**
 * Log event for a DHCP absolute event .
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_NAT_EVT_DHCP_ABS"
 * mutable="false"
 */
public class DhcpAbsoluteEvent extends LogEvent implements Serializable
{
    private List absoluteLeaseList = null;

    /**
     * Hibernate constructor
     */
    public DhcpAbsoluteEvent() {}

    public DhcpAbsoluteEvent( List s )
    {
        absoluteLeaseList = s;
    }

    /**
     * List of the absolute leases associated with the event.
     *
     * @return the list of the redirect rules.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_NAT_EVT_DHCP_ABS_LEASES"
     * @hibernate.collection-key
     * column="EVENT_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.tran.nat.DhcpAbsoluteLease"
     * column="LEASE_ID"
     */
    public List getAbsoluteLeaseList()
    {
        return absoluteLeaseList;
    }

    public void setAbsoluteLeaseList( List s )
    {
        absoluteLeaseList = s;
    }

    void addAbsoluteLease( DhcpAbsoluteLease lease )
    {
        if ( absoluteLeaseList == null ) {
            absoluteLeaseList = new LinkedList();
        }

        absoluteLeaseList.add( lease );
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("num-leases", absoluteLeaseList.size());

        DhcpAbsoluteLease absoluteLease;
        for (Iterator iter = absoluteLeaseList.iterator(); true == iter.hasNext(); )
        {
            absoluteLease = (DhcpAbsoluteLease) iter;
            absoluteLease.appendSyslog(sb);
        }
    }

    public String getSyslogId()
    {
        return "DHCP_AbsoluteLeases";
    }

    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL; // statistics or normal operation
    }
}
