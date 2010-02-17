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
