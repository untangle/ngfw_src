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

package com.untangle.uvm.node.firewall.ip;

import java.net.InetAddress;

import java.util.List;

import com.untangle.uvm.networking.IPNetwork;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;

import com.untangle.uvm.node.firewall.ParsingFactory;

/**
 * A factory for IP matchers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class IPMatcherFactory
{
    private static final IPMatcherFactory INSTANCE = new IPMatcherFactory();

    /** The parser used to translate strings into IntfDBMatchers. */
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
    
    /**
     * Update all of the matchers that depend on the configuration of the network
     * 
     * @param primaryAddress The primary address of the external interface.
     * @param externalAddressArray The array of addresses for the external interface
     */
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

    /**
     * Update the internal network with a list of networks.
     * 
     * @param networkList The list of networks that are on the internal interface.
     */    
    public final void setInternalNetworks( List<IPNetwork> networkList )
    {
        IPInternalMatcher.getInternalMatcher().setInternalNetworks( networkList );
    }

    public static final IPMatcherFactory getInstance()
    {
        return INSTANCE;
    }

    /**
     * Convert <param>value</param> to an IPDBMatcher.
     *
     * @param value The string to parse.
     */
    public static final IPDBMatcher parse( String value ) throws ParseException
    {
        return INSTANCE.factory.parse( value );
    }
    
    
}
