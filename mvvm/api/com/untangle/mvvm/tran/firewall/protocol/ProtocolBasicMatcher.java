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
 * The class <code>ProtocolMatcher</code> represents a class for filtering on the Protocol of a
 * session.
 *
 * @author <a href="mailto:rbscott@untangle.com">rbscott</a>
 * @version 1.0
 */
public final class ProtocolBasicMatcher extends ProtocolDBMatcher
{
    private static final long serialVersionUID = 2396418065775379605L;

    private static final Map<String,ProtocolBasicMatcher> nameToMatcherMap =
        new HashMap<String,ProtocolBasicMatcher>();

    private static final ProtocolBasicMatcher MATCHER_TCP =
        makeMatcher( MARKER_TCP, true, false, false );

    private static final ProtocolBasicMatcher MATCHER_UDP =
        makeMatcher( MARKER_UDP, false, true, false );

    private static final ProtocolBasicMatcher MATCHER_PING =
        makeMatcher( MARKER_PING, false, false, true );

    private static final ProtocolBasicMatcher MATCHER_TCP_AND_UDP =
        makeMatcher( MARKER_TCP_AND_UDP, true, true, false );

    private final String name;
    private final boolean tcp;
    private final boolean udp;
    private final boolean ping;

    private ProtocolBasicMatcher( String name, boolean tcp, boolean udp, boolean ping )
    {
        this.name = name;
        this.tcp  = tcp;
        this.udp  = udp;
        this.ping = ping;
    }

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

    public static ProtocolDBMatcher getTCPMatcher()
    {
        return MATCHER_TCP;
    }

    public static ProtocolDBMatcher getUDPMatcher()
    {
        return MATCHER_UDP;
    }

    public static ProtocolDBMatcher getPingMatcher()
    {
        return MATCHER_PING;
    }

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
