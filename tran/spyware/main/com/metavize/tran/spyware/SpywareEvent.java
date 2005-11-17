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
import com.metavize.mvvm.tran.PipelineEndpoints;

public abstract class SpywareEvent extends LogEvent
{
    // constructors -----------------------------------------------------------

    public SpywareEvent() { }

    // abstract methods -------------------------------------------------------

    public abstract String getReason();
    public abstract String getIdentification();
    public abstract String getLocation();
    public abstract boolean isBlocked();
    public abstract PipelineEndpoints getPipelineEndpoints();

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.addField("info", getIdentification());
        sb.addField("loc", getLocation());
        sb.addField("blocked", Boolean.toString(isBlocked()));
    }
}
