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

package com.untangle.uvm.node.firewall;

/**
 * A pool of constants used for parsing.  Modify these with caution,
 * because the database representation depends on these constants.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class ParsingConstants
{
    /* User representation of a matcher that should never match */
    public static final String MARKER_NOTHING   = "none";
    
    /* User representation of a matcher that should always return true */
    public static final String MARKER_ANY       = "any";

    /* User representation of a matcher that should always return true */
    public static final String MARKER_ALL       = "all";

    /* String for when a matcher is not applicable to the application. */
    public static final String MARKER_NA        = "n/a";

    /* Token separating a matcher that uses a range. */
    public static final String MARKER_RANGE     = "-";

    /* Token separating a matcher that always matches. */
    public static final String MARKER_WILDCARD  = "*";

    /* Token used to separate the values in a set matcher */
    public static final String MARKER_SEPERATOR = ",";

    /* Token used at the beginning of a matcher to signify it should
     * invert its matching algorithm */
    public static final String MARKER_INVERSE   = "~";
}
