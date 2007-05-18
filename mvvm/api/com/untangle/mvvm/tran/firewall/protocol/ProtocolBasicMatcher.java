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

package com.untangle.mvvm.tran.firewall.protocol;

import java.util.HashMap;
import java.util.Map;

import com.untangle.mvvm.tapi.Protocol;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;

import static com.untangle.mvvm.tran.firewall.protocol.ProtocolParsingConstants.MARKER_TCP;
import static com.untangle.mvvm.tran.firewall.protocol.ProtocolParsingConstants.MARKER_UDP;
import static com.untangle.mvvm.tran.firewall.protocol.ProtocolParsingConstants.MARKER_PING;
import static com.untangle.mvvm.tran.firewall.protocol.ProtocolParsingConstants.MARKER_TCP_AND_UDP;

/**
 * The class <code>ProtocolMatcher</code> represents a class for
 * filtering on the Protocol of a session.
 *
 * @author <a href="mailto:rbscott@untangle.com">rbscott</a>
 * @version 1.0
 */
public final class ProtocolBasicMatcher extends ProtocolDBMatcher
{
    private static final long serialVersionUID = 2396418065775379605L;

    /** Map from a string name to a protocol matcher */
    private static final Map<String,ProtocolBasicMatcher> nameToMatcherMap =
        new HashMap<String,ProtocolBasicMatcher>();

    /** This matches TCP traffic */
    private static final ProtocolBasicMatcher MATCHER_TCP =
        makeMatcher( MARKER_TCP, true, false, false );

    /** This matches UDP traffic */
    private static final ProtocolBasicMatcher MATCHER_UDP =
        makeMatcher( MARKER_UDP, false, true, false );

    /** This matches PING traffic */
    private static final ProtocolBasicMatcher MATCHER_PING =
        makeMatcher( MARKER_PING, false, false, true );

    /** This matches TCP and UDP traffic */
    private static final ProtocolBasicMatcher MATCHER_TCP_AND_UDP =
        makeMatcher( MARKER_TCP_AND_UDP, true, true, false );

    /* The database string for this matcher */
    private final String name;
    
    /* True if this matcher matches TCP */
    private final boolean tcp;

    /* True if this matcher matches UDP */
    private final boolean udp;

    /* True if this matcher matches PING session */
    private final boolean ping;

    private ProtocolBasicMatcher( String name, boolean tcp, boolean udp, boolean ping )
    {
        this.name = name;
        this.tcp  = tcp;
        this.udp  = udp;
        this.ping = ping;
    }

    
    /**
     * Test if <param>protocol<param> matches this matcher.
     *
     * @param protocol The protocol to test.
     * @return True if <param>protcol</param> matches.
     */
    public boolean isMatch( Protocol protocol )
    {
        if (( protocol == Protocol.TCP ) && this.tcp ) return true;

        if (( protocol == Protocol.UDP ) && this.udp ) return true;

        /* Right now Ping is a UDP session. [XXX ICMP HACK] */
        if (( protocol == Protocol.UDP ) && this.ping ) return true;

        /* The Traffic Matcher uses ICMP as the protocol */
        if (( protocol == Protocol.ICMP ) && this.ping ) return true;

        return false;
    }

    /**
     * Test if <param>protocol<param> matches this matcher.
     *
     * @param protocol The protocol to test.
     * @return True if <param>protcol</param> matches.
     */
    public boolean isMatch( short protocol )
    {
        if (( protocol == Protocol.TCP.getId() ) && this.tcp ) return true;

        if (( protocol == Protocol.UDP.getId() ) && this.udp ) return true;

        /* Right now Ping is a UDP session. [XXX ICMP HACK] */
        if (( protocol == Protocol.UDP.getId() ) && this.ping ) return true;

        /* The Traffic Matcher uses ICMP as the protocol */
        if (( protocol == Protocol.ICMP.getId() ) && this.ping ) return true;

        return false;
    }

    public String toDatabaseString()
    {
        return toString();
    }

    public String toString()
    {
        return this.name;
    }

    /**
     * Retrieve the TCP matcher.
     *
     * @return A matcher that matches TCP protocol.
     */
    public static ProtocolDBMatcher getTCPMatcher()
    {
        return MATCHER_TCP;
    }

    /**
     * Retrieve the UDP matcher.
     *
     * @return A matcher that matches UDP protocol.
     */
    public static ProtocolDBMatcher getUDPMatcher()
    {
        return MATCHER_UDP;
    }

    /**
     * Retrieve the ping matcher.
     *
     * @return A matcher that matches the ping protocol.
     */
    public static ProtocolDBMatcher getPingMatcher()
    {
        return MATCHER_PING;
    }

    /**
     * Retrieve the tcp and udp matcher.
     *
     * @return A matcher that matches tcp and udp protocol.
     */
    public static ProtocolDBMatcher getTCPAndUDPMatcher()
    {
        return MATCHER_TCP_AND_UDP;
    }

    private static ProtocolBasicMatcher makeMatcher( String name, boolean tcp, boolean udp, boolean ping )
    {
        ProtocolBasicMatcher matcher = nameToMatcherMap.get( name );

        /* If necessary add the matcher to the map */
        if ( matcher == null ) {
            matcher = new ProtocolBasicMatcher( name, tcp, udp, ping );
            nameToMatcherMap.put( name, matcher );
        }

        return matcher;
    }

    /* This is the parser for simple protocol matchers */
    static final Parser<ProtocolDBMatcher> PARSER = new Parser<ProtocolDBMatcher>()
    {
        public int priority()
        {
            return 10;
        }

        public boolean isParseable( String value )
        {
            value = value.trim();

            return ( nameToMatcherMap.get( value ) != null );
        }

        public ProtocolDBMatcher parse( String value ) throws ParseException
        {
            value = value.trim();

            if ( !isParseable( value )) {
                throw new ParseException( "Invalid protocol basic matcher '" + value + "'" );
            }

            ProtocolBasicMatcher matcher = nameToMatcherMap.get( value );

            if ( matcher == null ) {
                throw new ParseException( "Invalid protocol basic matcher '" + value + "'" );
            }

            return matcher;
        }
    };
}
