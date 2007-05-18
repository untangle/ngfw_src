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

import com.untangle.mvvm.tran.ParseException;

import com.untangle.mvvm.tran.firewall.ParsingFactory;

import static com.untangle.mvvm.tran.firewall.protocol.ProtocolParsingConstants.MARKER_TCP;
import static com.untangle.mvvm.tran.firewall.protocol.ProtocolParsingConstants.MARKER_UDP;
import static com.untangle.mvvm.tran.firewall.protocol.ProtocolParsingConstants.MARKER_PING;
import static com.untangle.mvvm.tran.firewall.protocol.ProtocolParsingConstants.MARKER_TCP_AND_UDP;
import static com.untangle.mvvm.tran.firewall.protocol.ProtocolParsingConstants.MARKER_ANY;

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
        MARKER_TCP_AND_UDP, MARKER_UDP, MARKER_TCP, MARKER_PING, MARKER_ANY
    };

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
