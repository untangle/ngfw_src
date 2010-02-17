/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node.firewall.time;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
