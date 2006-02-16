/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran.firewall.ip;

import java.net.InetAddress;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.ParseException;

import com.metavize.mvvm.tran.firewall.ParsingFactory;

public class IPMatcherFactory
{
    private static final IPMatcherFactory INSTANCE = new IPMatcherFactory();

    private final ParsingFactory<IPDBMatcher> factory;

    private IPMatcherFactory()
    {
        this.factory = new ParsingFactory<IPDBMatcher>( "intf matcher" );
        factory.registerParsers( IPSimpleMatcher.PARSER, IPLocalMatcher.PARSER, IPSingleMatcher.PARSER,
                                 IPSetMatcher.PARSER, IPRangeMatcher.PARSER,
                                 IPSubnetMatcher.PARSER );
    }

    /** This can't be stored into the DB */
    public final IPMatcher getInternalMatcher()
    {
        return IPInternalMatcher.getInternalMatcher();
    }
    
    /** This can't be stored into the DB */
    public final IPMatcher getExternalMatcher()
    {
        return IPInternalMatcher.getExternalMatcher();
    }

    public final IPDBMatcher getAllMatcher()
    {
        return IPSimpleMatcher.getAllMatcher();
    }

    public final IPDBMatcher getNilMatcher()
    {
        return IPSimpleMatcher.getNilMatcher();
    }

    public final IPDBMatcher makeSingleMatcher( IPaddr address )
    {
        return IPSingleMatcher.makeInstance( address );
    }

    public final IPDBMatcher makeSingleMatcher( InetAddress address )
    {
        return IPSingleMatcher.makeInstance( address );
    }


    public final IPDBMatcher makeSubnetMatcher( IPaddr network, IPaddr netmask )
    {
        return IPSubnetMatcher.makeInstance( network, netmask );
    }

    public final IPDBMatcher makeSubnetMatcher( InetAddress network, InetAddress netmask )
    {
        return IPSubnetMatcher.makeInstance( network, netmask );
    }

    public final IPDBMatcher makeSubnetMatcher( InetAddress network, int cidr ) throws ParseException
    {
        return IPSubnetMatcher.makeInstance( network, cidr );
    }

    public static final IPMatcherFactory getInstance()
    {
        return INSTANCE;
    }

    /* Shorcut method */
    public static final IPDBMatcher parse( String value ) throws ParseException
    {
        return INSTANCE.factory.parse( value );
    }
    
    
}
