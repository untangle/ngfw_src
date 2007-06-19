/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.logging;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.PatternParser;

/**
 * Layout for log messages produced in the system. Adds conversion
 * character D which outputs the date of the log event formatted as
 * 'dd HH:mm:ss,SSS'.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class UtPatternlayout extends PatternLayout
{
    static final String MV_DEFAULT_CONVERSION_PATTERN = "%D %-5p [%c{1}] %m%n";

    // constructors -----------------------------------------------------------

    public UtPatternlayout(String pattern)
    {
        super(pattern);
    }

    public UtPatternlayout()
    {
        this(MV_DEFAULT_CONVERSION_PATTERN);
    }

    // PatternLayout methods --------------------------------------------------

    @Override
    public PatternParser createPatternParser(String pattern)
    {
        String p = null == pattern ? MV_DEFAULT_CONVERSION_PATTERN : pattern;
        return new UtPatternParser(p);
    }
}
