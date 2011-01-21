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
import java.util.HashMap;
import java.util.Map;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

/**
 * Interface matcher that matches more than one interfaces.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public final class IntfSetMatcher extends IntfDBMatcher
{
    /* Cache of the created interface matchers */
    static Map<ImmutableBitSet,IntfSetMatcher> CACHE = new HashMap<ImmutableBitSet,IntfSetMatcher>();

    /* Set of bits that shouldn't match */
    private final ImmutableBitSet intfSet;

    /* String representation for the database */
    private final String databaseRepresentation;

    private IntfSetMatcher(ImmutableBitSet intfSet, String databaseRepresentation)
    {
        this.intfSet = intfSet;
        this.databaseRepresentation = databaseRepresentation;
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

        return intfSet.get(intf);
    }

    public String toDatabaseString()
    {
        return this.databaseRepresentation;
    }

    public String toString()
    {
        StringBuilder ur = new StringBuilder();

        IntfMatcherEnumeration ime = IntfMatcherEnumeration.getInstance();

        for (int intf = intfSet.nextSetBit(0); intf >= 0;
             intf = intfSet.nextSetBit(intf + 1)) {
            if (0 < ur.length()) {
                ur.append(" & ");
            }
            ur.append(ime.getIntfUserName(intf));
        }

        return ur.toString();
    }

    /**
     * Create an Set Matcher.
     *
     * @param intfArray array of interfaces that should match.
     */
    public static IntfDBMatcher makeInstance(int ... intfArray)
        throws ParseException
    {
        BitSet intfSet = new BitSet(IntfConstants.MAX_INTF);

        /* The first pass is to just fill the bitset */
        for (int intf : intfArray) intfSet.set(intf);

        StringBuilder dbr = new StringBuilder();

        /* Second pass, build up the user and database string */
        for (int intf = intfSet.nextSetBit(0); intf >= 0;
             intf = intfSet.nextSetBit(intf + 1)) {
            if (dbr.length() > 0) {
                dbr.append(",");
            }
            dbr.append(Integer.toString(intf));
        }

        return makeInstance(intfSet, dbr.toString());
    }

    /**
     * Parser for the set matcher.
     */
    static final Parser<IntfDBMatcher> PARSER = new Parser<IntfDBMatcher>()
    {
        /* This has to be after the inverse matcher, because isParseable will
         * pass since anything with the seperator will pass */
        public int priority()
        {
            return 4;
        }

        public boolean isParseable(String value)
        {
            return (value.contains(ParsingConstants.MARKER_SEPERATOR));
        }

        public IntfDBMatcher parse(String value) throws ParseException
        {
            if (!isParseable(value)) {
                throw new ParseException("Invalid intf set matcher '" + value
                                         + "'");
            }

            /* Split up the items */
            String databaseArray[] = value.split(ParsingConstants.MARKER_SEPERATOR);

            int intfArray[] = new int[databaseArray.length];

            int c = 0;
            for (String databaseString : databaseArray) {
                IntfMatcherEnumeration ime = IntfMatcherEnumeration.getInstance();
                intfArray[c++] = ime.parseInterface(databaseString);
            }

            return makeInstance(intfArray);
        }
    };

    /**
     * Create or retrieve the Interface matcher for this bit set.
     * Only create a new bitset if there isn't one in the cache.
     *
     * @param intfSet BitSet to use.
     * @param user User string for this interface matcher.
     * @param database Database representation for this bitset.
     */
    private static IntfDBMatcher makeInstance(BitSet intfSet, String database)
    {
        ImmutableBitSet i = new ImmutableBitSet(intfSet);
        IntfSetMatcher cache = CACHE.get(i);
        if (null == cache) {
            CACHE.put(i, cache = new IntfSetMatcher(i, database));
        }
        return cache;
    }
}
