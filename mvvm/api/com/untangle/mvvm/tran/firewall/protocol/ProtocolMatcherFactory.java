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

public class ProtocolMatcherFactory
{
    private static final ProtocolMatcherFactory INSTANCE = new ProtocolMatcherFactory();

    private final ParsingFactory<ProtocolDBMatcher> factory;

    private static final String[] ENUMERATION = { 
        MARKER_TCP_AND_UDP, MARKER_UDP, MARKER_TCP, MARKER_PING, MARKER_ANY
    };

    private ProtocolMatcherFactory()
    {
        this.factory = new ParsingFactory<ProtocolDBMatcher>( "protocol matcher" );
        factory.registerParsers( ProtocolSimpleMatcher.PARSER, ProtocolBasicMatcher.PARSER );
    }
        
    public final ProtocolDBMatcher getAllMatcher()
    {
        return ProtocolSimpleMatcher.getAllMatcher();
    }

    public final ProtocolDBMatcher getNilMatcher()
    {
        return ProtocolSimpleMatcher.getNilMatcher();
    }
    
    public final ProtocolDBMatcher getTCPMatcher()
    {
        return ProtocolBasicMatcher.getTCPMatcher();
    }

    public final ProtocolDBMatcher getUDPMatcher()
    {
        return ProtocolBasicMatcher.getUDPMatcher();
    }

    public final ProtocolDBMatcher getPingMatcher()
    {
        return ProtocolBasicMatcher.getPingMatcher();
    }

    public final ProtocolDBMatcher getTCPAndUDPMatcher()
    {
        return ProtocolBasicMatcher.getTCPAndUDPMatcher();
    }

    public static final ProtocolMatcherFactory getInstance()
    {
        return INSTANCE;
    }

    public static String[] getProtocolEnumeration()
    {
        return ENUMERATION;
    }

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
