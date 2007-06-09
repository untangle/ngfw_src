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
public class MvPatternLayout extends PatternLayout
{
    static final String MV_DEFAULT_CONVERSION_PATTERN = "%D %-5p [%c{1}] %m%n";

    // constructors -----------------------------------------------------------

    public MvPatternLayout(String pattern)
    {
        super(pattern);
    }

    public MvPatternLayout()
    {
        this(MV_DEFAULT_CONVERSION_PATTERN);
    }

    // PatternLayout methods --------------------------------------------------

    @Override
    public PatternParser createPatternParser(String pattern)
    {
        String p = null == pattern ? MV_DEFAULT_CONVERSION_PATTERN : pattern;
        return new MvPatternParser(p);
    }
}
