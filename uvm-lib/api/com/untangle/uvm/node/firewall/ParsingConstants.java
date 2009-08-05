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
    public static final String MARKER_NOTHING = "none";

    /* User representation of a matcher that should always return true */
    public static final String MARKER_ANY = "any";

    /* User representation of a matcher that should always return true */
    public static final String MARKER_ALL = "all";

    /* User representation of a matcher that should return true if
     * more internal than the other interface */
    public static final String MARKER_MORE_TRUSTED = "more_trusted";

    /* User representation of a matcher that should return true if
     * more external than the other interface */
    public static final String MARKER_LESS_TRUSTED = "less_trusted";

    /* User representation of a matcher that should return true if
     * the interface is a wan interface. */
    public static final String MARKER_WAN = "wan";

    /* User representation of a matcher that should return true if
     * the interface is not a wan interface */
    public static final String MARKER_NON_WAN = "non_wan";

    /* String for when a matcher is not applicable to the application. */
    public static final String MARKER_NA = "n/a";

    /* Token separating a matcher that uses a range. */
    public static final String MARKER_RANGE = "-";

    /* Token separating a matcher that always matches. */
    public static final String MARKER_WILDCARD = "*";

    /* Token used to separate the values in a set matcher */
    public static final String MARKER_SEPERATOR = ",";

    /* Token used at the beginning of a matcher to signify it should
     * invert its matching algorithm */
    public static final String MARKER_INVERSE = "~";
}
