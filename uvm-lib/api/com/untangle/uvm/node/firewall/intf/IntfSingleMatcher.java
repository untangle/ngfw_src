/* $HeadURL$ */
package com.untangle.uvm.node.firewall.intf;

import java.util.HashMap;
import java.util.Map;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

/**
 * An IntfMatcher that matches a single interface.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public final class IntfSingleMatcher extends IntfDBMatcher
{

    /* Interface matcher for the external interface */
    private static final IntfDBMatcher EXTERNAL_MATCHER;

    /* Interface matcher for the internal interface */
    private static final IntfDBMatcher INTERNAL_MATCHER;

    /* Interface matcher for the dmz interface */
    private static final IntfDBMatcher DMZ_MATCHER;

    /* Interface matcher for the vpn interface */
    private static final IntfDBMatcher OPENVPN_MATCHER;

    /* Map of interfaces to their matcher */
    static Map<Integer,IntfSingleMatcher> CACHE = new HashMap<Integer,IntfSingleMatcher>();

    /* The interface this matcher matches */
    private final int intf;

    private IntfSingleMatcher(int intf)
    {
        this.intf = intf;
    }

    /**
     * Test if <param>intf<param> matches this matcher.
     *
     * @param intf Interface to test.
     * @return True if the interface matches.
     */
    public boolean isMatch(int iface, int otherIface)
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
        return Integer.toString(intf);
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
     * Retrieve the OPENVPN matcher */
    public static IntfDBMatcher getVpnMatcher()
    {
        return OPENVPN_MATCHER;
    }

    public static IntfDBMatcher makeInstance(int intf)
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
        OPENVPN_MATCHER = makeInstance(IntfConstants.OPENVPN_INTF);
    }}
