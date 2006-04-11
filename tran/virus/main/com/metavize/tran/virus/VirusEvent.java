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
import com.metavize.mvvm.logging.SyslogPriority;
import com.metavize.mvvm.tran.PipelineEndpoints;

public abstract class VirusEvent extends LogEvent
{
    // action types
    public static final int PASSED = 0; // no infection or passed infection or
                                        // clean message or passed infected message
    public static final int CLEANED = 1; // cleaned infection or
                                         // removed infection from message
    public static final int BLOCKED = 2;

    // constructors -----------------------------------------------------------

    public VirusEvent() { }

    // abstract methods -------------------------------------------------------

    public abstract PipelineEndpoints getPipelineEndpoints();
    public abstract String getType();
    public abstract String getLocation();
    public abstract boolean isInfected();
    public abstract int getActionType();
    public abstract String getActionName();
    public abstract String getVirusName();

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
        sb.addField("location", getLocation());
        sb.addField("infected", isInfected());
        sb.addField("action", getActionName());
        sb.addField("virus-name", getVirusName());
    }

    public String getSyslogId()
    {
        return getType();
    }

    public SyslogPriority getSyslogPriority()
    {
        switch(getActionType())
        {
            case PASSED:
                // NOTICE = infected but passed
                // INFORMATIONAL = statistics or normal operation
                return true == isInfected() ? SyslogPriority.NOTICE : SyslogPriority.INFORMATIONAL;

            default:
            case CLEANED:
            case BLOCKED:
                return SyslogPriority.WARNING; // traffic altered
        }
    }
}
