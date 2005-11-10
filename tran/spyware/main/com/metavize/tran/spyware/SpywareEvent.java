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

import java.io.IOException;

import com.metavize.mvvm.logging.PipelineEvent;

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

    protected void doSyslog(Appendable a) throws IOException
    {
        a.append(" info: id=");
        a.append(getIdentification());
        a.append(", loc=");
        String loc = getLocation();
        a.append(loc, 0, Math.min(loc.length(), 256));
        a.append(", blocked=");
        a.append(Boolean.toString(isBlocked()));
        a.append(" #");
    }
}
