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

package com.untangle.mvvm.tran.firewall.ip;

import java.net.InetAddress;

import java.util.List;

import com.untangle.mvvm.networking.IPNetwork;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;

import com.untangle.mvvm.tran.firewall.ParsingFactory;

public class IPMatcherFactory
{
    private static final IPMatcherFactory INSTANCE = new IPMatcherFactory();

    private final ParsingFactory<IPDBMatcher> factory;

    private IPMatcherFactory()
    {
        this.factory = new ParsingFactory<IPDBMatcher>( "ip matcher" );
        factory.registerParsers( IPSimpleMatcher.PARSER, IPLocalMatcher.PARSER, IPAllPublicMatcher.PARSER,
                                 IPSingleMatcher.PARSER, IPSetMatcher.PARSER,   IPRangeMatcher.PARSER,
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

    public final IPDBMatcher getLocalMatcher()
    {
        return IPLocalMatcher.getInstance();
    }

    public final IPDBMatcher getAllPublicMatcher()
    {
        return IPAllPublicMatcher.getInstance();
    }
    
    public final void setLocalAddresses( InetAddress primaryAddress, InetAddress ... externalAddressArray )
    {
        if (( externalAddressArray == null ) || ( externalAddressArray.length == 0 )) {
            IPLocalMatcher.getInstance().setAddress( primaryAddress );
            IPAllPublicMatcher.getInstance().setAddresses( primaryAddress );
        } else {
            /* Add the primary address, it may not be in the external address array since,
             * it could be assigned by DHCP(it doesn't matter if it is in there twice, as this
             * is a set) */
            InetAddress addressArray[] = externalAddressArray;
                
            if ( primaryAddress != null ) {
                addressArray = new InetAddress[externalAddressArray.length + 1];
                addressArray[0] = primaryAddress;
                for ( int c = 0 ; c < externalAddressArray.length ; c++ ) {
                    addressArray[c+1] = externalAddressArray[c];
                }
            }
            
            IPLocalMatcher.getInstance().setAddress( primaryAddress );
            IPAllPublicMatcher.getInstance().setAddresses( addressArray );
        }
    }

    public final void setInternalNetworks( List<IPNetwork> networkList )
    {
        IPInternalMatcher.getInternalMatcher().setInternalNetworks( networkList );
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
