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

package com.metavize.tran.protofilter;

import com.metavize.mvvm.logging.PipelineEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;
import com.metavize.mvvm.tran.PipelineEndpoints;

/**
 * Log event for a proto filter match.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="TR_PROTOFILTER_EVT"
 * mutable="false"
 */
public class ProtoFilterLogEvent extends PipelineEvent
{
    private String protocol;
    private boolean blocked;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public ProtoFilterLogEvent() { }

    public ProtoFilterLogEvent(PipelineEndpoints pe, String protocol, boolean blocked)
    {
        super(pe);
        this.protocol = protocol;
        this.blocked = blocked;
    }

    // accessors --------------------------------------------------------------

    /**
     * The protocol, as determined by the protocol filter.
     *
     * @return the protocol name.
     * @hibernate.property
     * column="PROTOCOL"
     */
    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    /**
     * Whether or not we blocked it.
     *
     * @return whether or not the session was blocked (closed)
     * @hibernate.property
     * column="BLOCKED"
     */
    public boolean isBlocked()
    {
        return blocked;
    }

    public void setBlocked(boolean blocked)
    {
        this.blocked = blocked;
    }

    public void appendSyslog(SyslogBuilder sb)
    {
        getPipelineEndpoints().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("protocol", protocol);
        sb.addField("blocked", blocked);
    }

    public SyslogPriority getSyslogPrioritiy()
    {
        return blocked ? SyslogPriority.NOTICE : SyslogPriority.INFORMATIONAL;
    }
}
