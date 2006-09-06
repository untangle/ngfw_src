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

package com.metavize.tran.spyware;


import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;
import com.metavize.mvvm.tran.PipelineEndpoints;

public abstract class SpywareEvent extends LogEvent
{
    // constructors -----------------------------------------------------------

    public SpywareEvent() { }

    // abstract methods -------------------------------------------------------

    public abstract PipelineEndpoints getPipelineEndpoints();
    public abstract String getType();
    public abstract String getReason();
    public abstract String getIdentification();
    public abstract String getLocation();
    public abstract boolean isBlocked();

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getPipelineEndpoints().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("ident", getIdentification());
        sb.addField("loc", getLocation());
        sb.addField("blocked", isBlocked());
    }

    public String getSyslogId()
    {
        return getType();
    }

    public SyslogPriority getSyslogPriority()
    {
        // NOTICE = spyware (access, activeX, blacklist, cookie) event logged
        // WARNING = traffic altered
        return false == isBlocked() ? SyslogPriority.NOTICE : SyslogPriority.WARNING;
    }
}
