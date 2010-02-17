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

import java.util.HashMap;
import java.util.Map;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.node.InterfaceComparator;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

/**
 * An IntfMatcher that matches a single interface.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IntfSingleMatcher extends IntfDBMatcher
{
    private static final long serialVersionUID = -314023986591100152L;

    /* Interface matcher for the external interface */
    private static final IntfDBMatcher EXTERNAL_MATCHER;

    /* Interface matcher for the internal interface */
    private static final IntfDBMatcher INTERNAL_MATCHER;

    /* Interface matcher for the dmz interface */
    private static final IntfDBMatcher DMZ_MATCHER;

    /* Interface matcher for the vpn interface */
    private static final IntfDBMatcher VPN_MATCHER;

    /* Map of interfaces to their matcher */
    static Map<Byte,IntfSingleMatcher> CACHE = new HashMap<Byte,IntfSingleMatcher>();

    /* The interface this matcher matches */
    private final byte intf;

    private IntfSingleMatcher(byte intf)
    {
        this.intf = intf;
    }

    /**
     * Test if <param>intf<param> matches this matcher.
     *
     * @param intf Interface to test.
     * @return True if the interface matches.
     */
    public boolean isMatch(byte iface, byte otherIface, InterfaceComparator c)
    {
        /* These interfaces always match true */
        if (IntfConstants.UNKNOWN_INTF == iface
            || IntfConstants.LOOPBACK_INTF == iface) {
            return true;
        } else if (iface >= IntfConstants.MAX_INTF) {
            return false;
        } else {
            return (this.intf == iface);
        }
    }

    public String toDatabaseString()
    {
        return Byte.toString(intf);
    }

    public String toString()
    {
        return IntfMatcherEnumeration.getInstance().getIntfUserName(intf);
    }

    /**
     * Retrieve the External matcher */
    public static IntfDBMatcher getExternalMatcher()
    {
        return EXTERNAL_MATCHER;
    }

    /**
     * Retrieve the Internal matcher */
    public static IntfDBMatcher getInternalMatcher()
    {
        return INTERNAL_MATCHER;
    }

    /**
     * Retrieve the DMZ matcher */
    public static IntfDBMatcher getDmzMatcher()
    {
        return DMZ_MATCHER;
    }

    /**
     * Retrieve the VPN matcher */
    public static IntfDBMatcher getVpnMatcher()
    {
        return VPN_MATCHER;
    }

    public static IntfDBMatcher makeInstance(byte intf)
    {
        IntfSingleMatcher cache = CACHE.get(intf);

        if (null == cache) {
            cache = new IntfSingleMatcher(intf);
            CACHE.put(intf, cache);
        }

        return cache;
    }

    /* The parser for the single matcher */
    static final Parser<IntfDBMatcher> PARSER = new Parser<IntfDBMatcher>()
    {
        public int priority()
        {
            return 2;
        }

        public boolean isParseable(String value)
        {
            return (!value.contains(ParsingConstants.MARKER_SEPERATOR));
        }

        public IntfDBMatcher parse(String value) throws ParseException
        {
            if (!isParseable(value)) {
                throw new ParseException("Invalid intf single matcher '" + value + "'");
            }

            IntfMatcherEnumeration ime = IntfMatcherEnumeration.getInstance();
            return makeInstance(ime.parseInterface(value));
        }
    };

    static
    {
        EXTERNAL_MATCHER = makeInstance(IntfConstants.EXTERNAL_INTF);
        INTERNAL_MATCHER = makeInstance(IntfConstants.INTERNAL_INTF);
        DMZ_MATCHER = makeInstance(IntfConstants.DMZ_INTF);
        VPN_MATCHER = makeInstance(IntfConstants.VPN_INTF);
    }}
