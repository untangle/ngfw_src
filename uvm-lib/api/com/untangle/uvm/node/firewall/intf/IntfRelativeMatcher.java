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

import com.untangle.uvm.node.InterfaceComparator;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

/**
 * An interface matcher that matches an interface with a position
 * relative to the other interface.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public final class IntfRelativeMatcher extends IntfDBMatcher
{
    
    private static final IntfDBMatcher LESS_TRUSTED_MATCHER
        = new IntfRelativeMatcher(false);
    private static final IntfDBMatcher MORE_TRUSTED_MATCHER
        = new IntfRelativeMatcher(true);

    /* true if this is the all matcher */
    private final boolean moreTrusted;

    private IntfRelativeMatcher(boolean moreTrusted)
    {
        this.moreTrusted = moreTrusted;
    }

    /**
     * Test if <param>intf<param> matches this matcher.
     *
     * @param intf Interface to test.
     */
    public boolean isMatch(byte intf, byte otherIntf, InterfaceComparator c)
    {
        return moreTrusted ? c.isMoreTrusted(intf, otherIntf)
            : c.isLessTrusted(intf, otherIntf);
    }

    public String toDatabaseString()
    {
        return moreTrusted ? ParsingConstants.MARKER_MORE_TRUSTED
            : ParsingConstants.MARKER_LESS_TRUSTED;
    }

    public String toString()
    {
        return moreTrusted ? "More Trusted" : "Less Trusted";
    }

    /**
     * Retrieve the more_trusted matcher.
     *
     * @return An interface matcher that matches if this interface is
     * more internal than the other interface.
     */
    public static IntfDBMatcher getMoreTrustedMatcher()
    {
        return MORE_TRUSTED_MATCHER;
    }

    /**
     * Retrieve the less_trusted matcher.
     *
     * @return An interface matcher that matches if the interface is
     * more external than the other interface.
     */
    public static IntfDBMatcher getLessTrustedMatcher()
    {
        return LESS_TRUSTED_MATCHER;
    }

    /* The parse for simple matchers */
    static final Parser<IntfDBMatcher> PARSER = new Parser<IntfDBMatcher>()
    {
        /* This has the most specific syntax and should alyways first */
        public int priority()
        {
            return 1;
        }

        public boolean isParseable( String value )
        {
            return value.equalsIgnoreCase(ParsingConstants.MARKER_MORE_TRUSTED)
            || value.equalsIgnoreCase(ParsingConstants.MARKER_LESS_TRUSTED);
        }

        public IntfDBMatcher parse(String value) throws ParseException
        {
            if (!isParseable(value)) {
                throw new ParseException("Invalid intf simple matcher '"
                                         + value + "'");
            } else if (value.equalsIgnoreCase(ParsingConstants.MARKER_LESS_TRUSTED)) {
                return LESS_TRUSTED_MATCHER;
            } else if (value.equalsIgnoreCase(ParsingConstants.MARKER_MORE_TRUSTED)) {
                return MORE_TRUSTED_MATCHER;
            } else {
                throw new ParseException("Invalid intf simple matcher '" + value + "'");
            }
        }
    };
}

