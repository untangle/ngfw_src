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

package com.untangle.uvm.node.firewall.protocol;

import java.util.HashMap;
import java.util.Map;

import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;

import static com.untangle.uvm.node.firewall.protocol.ProtocolParsingConstants.MARKER_TCP;
import static com.untangle.uvm.node.firewall.protocol.ProtocolParsingConstants.MARKER_UDP;
import static com.untangle.uvm.node.firewall.protocol.ProtocolParsingConstants.MARKER_PING;
import static com.untangle.uvm.node.firewall.protocol.ProtocolParsingConstants.MARKER_TCP_AND_UDP;

/**
 * The class <code>ProtocolMatcher</code> represents a class for
 * filtering on the Protocol of a session.
 *
 * @author <a href="mailto:rbscott@untangle.com">rbscott</a>
 * @version 1.0
 */
public enum ProtocolBasicMatcher implements ProtocolDBMatcher
{
    /** This matches TCP traffic */
    MATCHER_TCP( MARKER_TCP, true, false, false ),
    /** This matches UDP traffic */
    MATCHER_UDP( MARKER_UDP, false, true, false ),
    /** This matches PING traffic */
    MATCHER_PING( MARKER_PING, false, false, true ),
    /** This matches TCP and UDP traffic */
    MATCHER_TCP_AND_UDP( MARKER_TCP_AND_UDP, true, true, false );
    
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

    public String getName() {
        return name;
    }

    public static ProtocolBasicMatcher getInstance(String name)
    {
        ProtocolBasicMatcher[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getName().equals(name)){
                return values[i];
            }
        }
        return null;
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

            return ( ProtocolBasicMatcher.getInstance( value ) != null );
        }

        public ProtocolDBMatcher parse( String value ) throws ParseException
        {
            value = value.trim();

            if ( !isParseable( value )) {
                throw new ParseException( "Invalid protocol basic matcher '" + value + "'" );
            }

            ProtocolBasicMatcher matcher = ProtocolBasicMatcher.getInstance( value );

            if ( matcher == null ) {
                throw new ParseException( "Invalid protocol basic matcher '" + value + "'" );
            }

            return matcher;
        }
    };
}
