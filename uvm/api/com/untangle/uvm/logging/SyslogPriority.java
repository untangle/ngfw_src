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

package com.untangle.uvm.logging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SyslogPriority implements Serializable
{
    public static final SyslogPriority EMERGENCY;
    public static final SyslogPriority ALERT;
    public static final SyslogPriority CRITICAL;
    public static final SyslogPriority ERROR;
    public static final SyslogPriority WARNING;
    public static final SyslogPriority NOTICE;
    public static final SyslogPriority INFORMATIONAL;
    public static final SyslogPriority DEBUG;

    private static final SyslogPriority[] priorities;

    static {
        priorities = new SyslogPriority[8];

        EMERGENCY = new SyslogPriority(0, "emergency");
        ALERT = new SyslogPriority(1, "alert");
        CRITICAL = new SyslogPriority(2, "critical");
        ERROR = new SyslogPriority(3, "error");
        WARNING = new SyslogPriority(4, "warning");
        NOTICE = new SyslogPriority(5, "notice");
        INFORMATIONAL = new SyslogPriority(6, "informational");
        DEBUG = new SyslogPriority(7, "debug");
    }

    private final int priorityCode;
    private final String name;

    // constructors -----------------------------------------------------------

    private SyslogPriority(int priorityCode, String name)
    {
        this.priorityCode = priorityCode;
        this.name = name;

        priorities[priorityCode] = this;
    }

    // static methods ---------------------------------------------------------

    public static SyslogPriority getPriority(int priorityCode)
    {
        return priorities[priorityCode];
    }

    public static SyslogPriority getPriority(String name)
    {
        for( SyslogPriority sp : priorities ){ // XXX linear, not fast but works for now
            if( sp.getName().equals(name) )
                return sp;
        }
        return null;
    }

    public static List<SyslogPriority> values()
    {
        List<SyslogPriority> l = new ArrayList<SyslogPriority>(priorities.length);
        for (SyslogPriority sp : priorities) {
            l.add(sp);
        }

        return l;
    }

    // business methods -------------------------------------------------------

    public boolean inThreshold(LogEvent le)
    {
        return inThreshold(le.getSyslogPriority());
    }

    public boolean inThreshold(SyslogPriority lp)
    {
        return lp.priorityCode <= priorityCode;
    }

    // accessors --------------------------------------------------------------

    public int getPriorityValue()
    {
        return priorityCode;
    }

    public String getName()
    {
        return name;
    }

    // serialization methods --------------------------------------------------

    Object readResolve()
    {
        return priorities[priorityCode];
    }
}
