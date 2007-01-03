/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine;

import com.untangle.mvvm.logging.LogEvent;

class LogEventDesc
{
    private final EventLoggerImpl eventLogger;
    private final LogEvent logEvent;
    private final String tag;

    LogEventDesc(EventLoggerImpl eventLogger, LogEvent logEvent, String tag)
    {
        this.eventLogger = eventLogger;
        this.logEvent = logEvent;
        this.tag = tag;
    }

    EventLoggerImpl getEventLogger()
    {
        return eventLogger;
    }

    LogEvent getLogEvent()
    {
        return logEvent;
    }

    String getTag()
    {
        return tag;
    }
}
