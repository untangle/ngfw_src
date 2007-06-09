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

package com.untangle.uvm.node.firewall.time;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.io.Serializable;

public abstract class DayOfWeekDBMatcher implements DayOfWeekMatcher, Serializable
{
    private static final long serialVersionUID = -6040354039760824420L;

    /** Package protected so that only classes in the package can add to the list
     * of database saveable dayOfWeek matchers */
    DayOfWeekDBMatcher()
    {
    }

    public abstract boolean isMatch( Date when );

    public String toDatabaseString() {
        // Kinda yucky, but...
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String s : toDatabaseList()) {
            if (i > 0)
                sb.append(DayOfWeekMatcherConstants.MARKER_SEPERATOR);
            sb.append(s);
            i++;
        }
        return sb.toString();
    }

    /* These lists are typically not modifiable */
    public abstract List<String> toDatabaseList();

    protected static boolean matchCanonicalDayString(String dayString, Date when)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(when);
        int calDay = cal.get(Calendar.DAY_OF_WEEK);
        switch (calDay) {
        case Calendar.MONDAY:
            return (dayString.equals("Monday"));
        case Calendar.TUESDAY:
            return (dayString.equals("Tuesday"));
        case Calendar.WEDNESDAY:
            return (dayString.equals("Wednesday"));
        case Calendar.THURSDAY:
            return (dayString.equals("Thursday"));
        case Calendar.FRIDAY:
            return (dayString.equals("Friday"));
        case Calendar.SATURDAY:
            return (dayString.equals("Saturday"));
        case Calendar.SUNDAY:
            return (dayString.equals("Sunday"));
        default:
            return false;
        }
    }
            
    protected static String canonicalizeDayName(String input)
    {
        if ("Monday".equalsIgnoreCase(input) || "mon".equalsIgnoreCase(input))
            return "Monday";
        if ("Tuesday".equalsIgnoreCase(input) || "tue".equalsIgnoreCase(input) || "tues".equalsIgnoreCase(input))
            return "Tuesday";
        if ("Wednesday".equalsIgnoreCase(input) || "wed".equalsIgnoreCase(input) || "weds".equalsIgnoreCase(input))
            return "Wednesday";
        if ("Thursday".equalsIgnoreCase(input) || "thu".equalsIgnoreCase(input) || "thurs".equalsIgnoreCase(input))
            return "Thursday";
        if ("Friday".equalsIgnoreCase(input) || "fri".equalsIgnoreCase(input))
            return "Friday";
        if ("Saturday".equalsIgnoreCase(input) || "sat".equalsIgnoreCase(input))
            return "Saturday";
        if ("Sunday".equalsIgnoreCase(input) || "sun".equalsIgnoreCase(input))
            return "Sunday";
        return null;
    }
}
