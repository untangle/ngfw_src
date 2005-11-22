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

package com.metavize.tran.virus;


import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.tran.PipelineEndpoints;

public abstract class VirusEvent extends LogEvent
{
    // constructors -----------------------------------------------------------

    public VirusEvent() { }

    // abstract methods -------------------------------------------------------

    public abstract String getType();
    public abstract String getLocation();
    public abstract boolean isInfected();
    public abstract String getActionName();
    public abstract String getVirusName();
    public abstract PipelineEndpoints getPipelineEndpoints();

    // accessors --------------------------------------------------------------

    public String getTraffic()
    {
        return "(" + getType() + ") " + getLocation();
    }

    public String getReason()
    {
        return isInfected() ? "virus found" : "no virus found";
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getPipelineEndpoints().appendSyslog(sb);
        sb.startSection("info");
        sb.addField("info", getLocation());
        sb.addField("infected", Boolean.toString(isInfected()));
        sb.addField("action", getActionName());
        sb.addField("virus-name", getVirusName());
    }
}
