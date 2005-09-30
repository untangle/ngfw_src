/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran.firewall;

import org.apache.log4j.Logger;

import com.metavize.mvvm.argon.IntfConverter;

public class InterfaceStaticRedirect extends InterfaceRedirect
{
    private final byte argonIntf;
    private static final Logger logger = Logger.getLogger( InterfaceStaticRedirect.class );

   /* Null matcher, these are automatically removed before adds */
    private static final InterfaceRedirect NIL_REDIRECT = 
        new InterfaceStaticRedirect( ProtocolMatcher.MATCHER_NIL,
                                     IntfMatcher.getMatcher( 0 ), IntfMatcher.getMatcher( 0 ),
                                     IPMatcher.MATCHER_NIL,       IPMatcher.MATCHER_NIL,
                                     PortMatcher.MATCHER_NIL,     PortMatcher.MATCHER_NIL, (byte)0 );

    public InterfaceStaticRedirect( ProtocolMatcher protocol, 
                                    IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                                    IPMatcher   srcAddress, IPMatcher       dstAddress,
                                    PortMatcher srcPort,    PortMatcher     dstPort,
                                    byte argonIntf )
    {
        /* InterfaceRedirects are always active */
        super( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );
        
        this.argonIntf = argonIntf;
    }

    public byte netcapIntf()
    {
        return IntfConverter.toNetcap( this.argonIntf );
    }

    public byte argonIntf()
    {
        return this.argonIntf;
    }
    
    public static InterfaceRedirect getNilRedirect()
    {
        return NIL_REDIRECT;
    }
}
