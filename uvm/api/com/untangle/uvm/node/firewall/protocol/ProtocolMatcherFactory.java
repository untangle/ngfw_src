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

import static com.untangle.uvm.node.firewall.protocol.ProtocolParsingConstants.MARKER_ANY;
import static com.untangle.uvm.node.firewall.protocol.ProtocolParsingConstants.MARKER_TCP;
import static com.untangle.uvm.node.firewall.protocol.ProtocolParsingConstants.MARKER_TCP_AND_UDP;
import static com.untangle.uvm.node.firewall.protocol.ProtocolParsingConstants.MARKER_UDP;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.ParsingFactory;

/**
 * A factory for protocol matchers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class ProtocolMatcherFactory
{
    private static final ProtocolMatcherFactory INSTANCE = new ProtocolMatcherFactory();

    private final ParsingFactory<ProtocolDBMatcher> factory;

    /* This is the enumeration of all of the available matchers */
    private static final String[] ENUMERATION = {
        MARKER_TCP_AND_UDP, MARKER_UDP, MARKER_TCP, MARKER_ANY
    };

    @SuppressWarnings("unchecked") //varargs
    private ProtocolMatcherFactory()
    {
        this.factory = new ParsingFactory<ProtocolDBMatcher>( "protocol matcher" );
        factory.registerParsers( ProtocolSimpleMatcher.PARSER, ProtocolBasicMatcher.PARSER );
    }

    /**
     * Retrieve the all matcher.
     *
     * @return A matcher that matches all protocols.
     */
    public final ProtocolDBMatcher getAllMatcher()
    {
        return ProtocolSimpleMatcher.getAllMatcher();
    }

    /**
     * Retrieve the nil matcher.
     *
     * @return A matcher that never matches a protocol.
     */
    public final ProtocolDBMatcher getNilMatcher()
    {
        return ProtocolSimpleMatcher.getNilMatcher();
    }

    /**
     * Retrieve the TCP matcher.
     *
     * @return A matcher that matches TCP.
     */
    public final ProtocolDBMatcher getTCPMatcher()
    {
        return ProtocolBasicMatcher.getTCPMatcher();
    }

    /**
     * Retrieve the UDP matcher.
     *
     * @return A matcher that matches UDP.
     */
    public final ProtocolDBMatcher getUDPMatcher()
    {
        return ProtocolBasicMatcher.getUDPMatcher();
    }

    /**
     * Retrieve the Ping matcher.
     *
     * @return A matcher that matches Ping.
     */
    public final ProtocolDBMatcher getPingMatcher()
    {
        return ProtocolBasicMatcher.getPingMatcher();
    }

    /**
     * Retrieve the TCP and UDP matcher.
     *
     * @return A matcher that matches TCP and UDP.
     */
    public final ProtocolDBMatcher getTCPAndUDPMatcher()
    {
        return ProtocolBasicMatcher.getTCPAndUDPMatcher();
    }

    public static final ProtocolMatcherFactory getInstance()
    {
        return INSTANCE;
    }

    /**
     * Retrieve an enumeration of all of the valid protocol matcher
     * strings.
     *
     * @return An array of all of the parseable protocol matcher strings.
     */
    public static String[] getProtocolEnumeration()
    {
        return ENUMERATION;
    }

    /**
     * Retrieve the default protocol matcher.
     *
     * @return The default protocol matcher.
     */
    public static String getProtocolDefault()
    {
        return ENUMERATION[0];
    }

    /* Shorcut method */
    public static final ProtocolDBMatcher parse( String value ) throws ParseException
    {
        return INSTANCE.factory.parse( value );
    }


}
