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

package com.untangle.uvm.node.firewall.intf;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

/**
 * An IntfMatcher that matches the simple cases (all or nothing).
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public final class IntfSimpleMatcher extends IntfDBMatcher
{

    /* An interface matcher that matches everything */
    private static final IntfDBMatcher ALL_MATCHER = new IntfSimpleMatcher(true);

    /* An interface matcher that doesn't match anything */
    private static final IntfDBMatcher NOTHING_MATCHER = new IntfSimpleMatcher(false);

    /* true if this is the all matcher */
    private final boolean isAll;

    private IntfSimpleMatcher(boolean isAll)
    {
        this.isAll = isAll;
    }

    /**
     * Test if <param>intf<param> matches this matcher.
     *
     * @param intf Interface to test.
     */
    public boolean isMatch(byte intf, byte otherIntf)
    {
        return isAll;
    }

    public String toDatabaseString()
    {
        return toString();
    }

    public String toString()
    {
        if (isAll) return ParsingConstants.MARKER_ANY;
        return ParsingConstants.MARKER_NOTHING;
    }

    public boolean isAllMatcher()
    {
        return isAll;
    }

    /**
     * Retrieve the all matcher
     *
     * @return An interface matcher that matches everything
     */
    public static IntfDBMatcher getAllMatcher()
    {
        return ALL_MATCHER;
    }

    /**
     * Retrieve the nil matcher
     *
     * @return An interface matcher that doesn't match anything.
     */
    public static IntfDBMatcher getNilMatcher()
    {
        return NOTHING_MATCHER;
    }

    /* The parse for simple matchers */
    static final Parser<IntfDBMatcher> PARSER = new Parser<IntfDBMatcher>()
    {
        /* This has the most specific syntax and should alyways first */
        public int priority()
        {
            return 0;
        }

        public boolean isParseable(String value)
        {
            return (value.equalsIgnoreCase(ParsingConstants.MARKER_ANY) ||
                     value.equalsIgnoreCase(ParsingConstants.MARKER_WILDCARD) ||
                     value.equalsIgnoreCase(ParsingConstants.MARKER_ALL) ||
                     value.equalsIgnoreCase(ParsingConstants.MARKER_NOTHING));
        }

        public IntfDBMatcher parse(String value) throws ParseException
        {
            if (!isParseable(value)) {
                throw new ParseException("Invalid intf simple matcher '" + value + "'");
            }

            if (value.equalsIgnoreCase(ParsingConstants.MARKER_ANY) ||
                 value.equalsIgnoreCase(ParsingConstants.MARKER_WILDCARD) ||
                 value.equalsIgnoreCase(ParsingConstants.MARKER_ALL)) {
                     return ALL_MATCHER;
                 } else if (value.equalsIgnoreCase(ParsingConstants.MARKER_NOTHING)) {
                     return NOTHING_MATCHER;
                 }

            throw new ParseException("Invalid intf simple matcher '" + value + "'");
        }
    };
}

