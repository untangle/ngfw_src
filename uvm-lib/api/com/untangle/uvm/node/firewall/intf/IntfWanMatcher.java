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

import java.util.BitSet;

import com.untangle.uvm.IntfConstants;
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
public final class IntfWanMatcher extends IntfDBMatcher
{

    /* An interface matcher that matches everything */
    private static final IntfDBMatcher WAN_MATCHER = new IntfWanMatcher(true);

    /* An interface matcher that doesn't match anything */
    private static final IntfDBMatcher NON_WAN_MATCHER = new IntfWanMatcher(false);

    /* true if this is the all matcher */
    private final boolean isWanMatcher;

    private static BitSet WAN_BIT_SET = new BitSet(IntfConstants.MAX_INTF);

    private IntfWanMatcher(boolean isWanMatcher)
    {
        this.isWanMatcher = isWanMatcher;
    }

    /**
     * Test if <param>intf<param> matches this matcher.
     *
     * @param intf Interface to test.
     */
    public boolean isMatch(int intf, int otherIntf)
    {
        /* This always matches true */
        if (IntfConstants.UNKNOWN_INTF == intf
            || IntfConstants.LOOPBACK_INTF == intf) {
            return true;
        } else if (intf >= IntfConstants.MAX_INTF) {
            return false;
        }

        return (!this.isWanMatcher) ^ WAN_BIT_SET.get(intf);
    }

    public String toDatabaseString()
    {
        return toString();
    }

    public String toString()
    {
        if (isWanMatcher) return ParsingConstants.MARKER_WAN;
        return ParsingConstants.MARKER_NON_WAN;
    }

    public boolean isWanMatcher()
    {
        return isWanMatcher;
    }
    
    static void setWanBitSet( BitSet newValue )
    {
        WAN_BIT_SET = (BitSet)newValue.clone();
    }

    /**
     * Retrieve the WAN Matcher
     *
     * @return An interface matcher that matches WAN interfaces
     */
    public static IntfDBMatcher getWanMatcher()
    {
        return WAN_MATCHER;
    }

    /**
     * Retrieve the non-wan matcher
     *
     * @return An interface matcher that matches non-wan interfaces
     */
    public static IntfDBMatcher getNonWanMatcher()
    {
        return NON_WAN_MATCHER;
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
            return (value.equalsIgnoreCase(ParsingConstants.MARKER_NON_WAN) ||
                    value.equalsIgnoreCase(ParsingConstants.MARKER_WAN));
        }

        public IntfDBMatcher parse(String value) throws ParseException
        {
            if (!isParseable(value)) {
                throw new ParseException("Invalid intf simple matcher '" + value + "'");
            }

            if (value.equalsIgnoreCase(ParsingConstants.MARKER_NON_WAN)) {
                return NON_WAN_MATCHER;
            } else if (value.equalsIgnoreCase(ParsingConstants.MARKER_WAN)) {
                return WAN_MATCHER;
            }
            throw new ParseException("Invalid intf wan matcher '" + value + "'");
        }
    };
}

