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

package com.untangle.uvm.logging;

import org.apache.log4j.Level;

/**
 * Represents syslog priority levels.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public enum SyslogPriority
{
    EMERGENCY(0, "emergency"),
    ALERT(1, "alert"),
    CRITICAL(2, "critical"),
    ERROR(3, "error"),
    WARNING(4, "warning"),
    NOTICE(5, "notice"),
    INFORMATIONAL(6, "informational"),
    DEBUG(7, "debug");

    private final int priorityCode;
    private final String name;

    // constructors -----------------------------------------------------------

    private SyslogPriority(int priorityCode, String name)
    {
        this.priorityCode = priorityCode;
        this.name = name;
    }

    // static methods ---------------------------------------------------------

    public static SyslogPriority getPriority(int priorityCode)
    {
    	SyslogPriority[] values = values();
    	for (int i = 0; i < values.length; i++) {
    		if (values[i].getPriorityValue() == priorityCode){
    			return values[i];
    		}
		}
    	return null;
    }

    public static SyslogPriority getPriority(String name)
    {
    	SyslogPriority[] values = values();
    	for (int i = 0; i < values.length; i++) {
    		if (values[i].getName().equals(name)){
    			return values[i];
    		}
		}
    	return null;
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

    public Level getLevel()
    {
        Level l = null;
        if (name.equalsIgnoreCase("debug"))
            l = Level.DEBUG;
        else if (name.equalsIgnoreCase("informational"))
            l = Level.INFO;
        else if (name.equalsIgnoreCase("notice"))
            l = Level.INFO;
        else if (name.equalsIgnoreCase("warning"))
            l = Level.WARN;
        else if (name.equalsIgnoreCase("error"))
            l = Level.ERROR;
        else if (name.equalsIgnoreCase("critical"))
            l = Level.ERROR;
        else if (name.equalsIgnoreCase("alert"))
            l = Level.ERROR;
        else if (name.equalsIgnoreCase("emergency"))
            l = Level.ERROR;
        else
            l = Level.INFO;

        return l;
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

}
