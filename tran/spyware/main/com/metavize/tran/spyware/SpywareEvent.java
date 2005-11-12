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


import com.metavize.mvvm.logging.PipelineEvent;
import com.metavize.mvvm.logging.SyslogBuilder;

public abstract class SpywareEvent extends PipelineEvent
{
    // constructors -----------------------------------------------------------

    public SpywareEvent() { }

    public SpywareEvent(int sessionId)
    {
        super(sessionId);
    }

    // abstract methods -------------------------------------------------------

    public abstract String getReason();
    public abstract String getIdentification();
    public abstract String getLocation();
    public abstract boolean isBlocked();

    // Syslog methods ---------------------------------------------------------

    protected void doSyslog(SyslogBuilder sb)
    {
        sb.addField("info", getIdentification());
        sb.addField("loc", getLocation());
        sb.addField("blocked", Boolean.toString(isBlocked()));
    }
}
