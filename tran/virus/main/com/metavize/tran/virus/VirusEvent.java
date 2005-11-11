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

import java.io.IOException;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tran.PipelineEndpoints;

public abstract class VirusEvent extends LogEvent
{
    private VirusScannerResult result;

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

    protected void doSyslog(Appendable a) throws IOException
    {
        a.append(" info: loc=");
        String loc = getLocation();
        a.append(loc, 0, Math.min(loc.length(), 256));

        a.append(", infected=");
        a.append(Boolean.toString(isInfected()));

        a.append(", action=");
        a.append(getActionName());

        a.append(", virus-name=");
        a.append(getVirusName());

        a.append(" #");
    }
}
