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

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.argon.ArgonException;


public class InterfaceRedirect extends TrafficIntfMatcher
{
    private final byte argonIntf;
    private static final Logger logger = Logger.getLogger( InterfaceRedirect.class );

    /* Null matcher, these are automatically removed before adds */
    private static final InterfaceRedirect NIL_REDIRECT = 
        new InterfaceRedirect( ProtocolMatcher.MATCHER_NIL,
                               IntfMatcher.getMatcher( 0 ), IntfMatcher.getMatcher( 0 ),
                               IPMatcher.MATCHER_NIL,       IPMatcher.MATCHER_NIL,
                               PortMatcher.MATCHER_NIL,     PortMatcher.MATCHER_NIL, (byte)0 );

    public InterfaceRedirect( ProtocolMatcher protocol, 
                              IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                              IPMatcher   srcAddress, IPMatcher       dstAddress,
                              PortMatcher srcPort,    PortMatcher     dstPort,
                              byte argonIntf )
    {
        /* InterfaceRedirects are always active */
        super( true, protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );
        
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

    /* Creates an interface redirect by looking up the interface that the new destination
     * would go out on, may want to update these every few hours??? */
    public static InterfaceRedirect makeInterfaceRedirect( TrafficIntfMatcher matcher,  
                                                           InetAddress newDestination )
    {
        /* Lookup the interface for the new destination */
        try {
            byte newIntf = MvvmContextFactory.context().argonManager().
                getOutgoingInterface( newDestination );

            if ( newIntf == IntfConverter.LOCALHOST ) {
                logger.error( "Redirect is destined to local host, matching sessions will be dropped." );
            }
            
            logger.info( "Redirect is going out interface " + newIntf );
            
            return new InterfaceRedirect( matcher.protocol, matcher.srcIntf, matcher.dstIntf,
                                          matcher.srcAddress, matcher.dstAddress,
                                          matcher.srcPort, matcher.dstPort, newIntf );
        } catch ( ArgonException e ) {
            /* XXXX This probably should be info eventually XXX also perform this operation before saving,
             * so the user can know that this will be a problem. */
            logger.warn( "Cannot create an interface redirect since the destination interface for " + 
                         newDestination + " cannot be determined", e );
        }

        return NIL_REDIRECT;
    }
}
