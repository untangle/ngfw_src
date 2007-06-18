/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.router;

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.IPaddr;

import com.untangle.uvm.networking.IPNetwork;
import com.untangle.uvm.networking.NetworkUtil;
import com.untangle.uvm.networking.RedirectRule;

import com.untangle.uvm.node.firewall.intf.IntfDBMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcherFactory;

class RouterUtil
{
    static final RouterUtil INSTANCE = new RouterUtil();

    static final IPaddr DEFAULT_NAT_ADDRESS = NetworkUtil.DEFAULT_NAT_ADDRESS;
    static final IPaddr DEFAULT_NAT_NETMASK = NetworkUtil.DEFAULT_NAT_NETMASK;
    
    static final IPaddr DEFAULT_DMZ_ADDRESS;
    
    static final IPaddr DEFAULT_DHCP_START = NetworkUtil.DEFAULT_DHCP_START;
    static final IPaddr DEFAULT_DHCP_END   = NetworkUtil.DEFAULT_DHCP_END;

    /* These are values for setup */
    static final IPaddr SETUP_INTERNAL_ADDRESS;
    static final IPaddr SETUP_INTERNAL_SUBNET;
    static final IPaddr SETUP_DHCP_START;
    static final IPaddr SETUP_DHCP_END;
    
    static final List<IPDBMatcher> LOCAL_MATCHER_LIST;

    /* Four hours, this parameter is actually unused */
    static final int DEFAULT_LEASE_TIME_SEC = NetworkUtil.DEFAULT_LEASE_TIME_SEC;

    private final Logger logger = Logger.getLogger( this.getClass());

    private RouterUtil()
    {
    }

    List<RedirectRule> getGlobalRedirectList( List<RedirectRule> fullList )
    {
        List<RedirectRule> list = new LinkedList<RedirectRule>();
        
        for ( RedirectRule item : fullList ) {
            if ( !item.isLocalRedirect()) list.add( item );
        }
        
        return list;
    }
    
    /* This removes all of the global items from full list and replaces them with the items
     * in global list */
    List<RedirectRule> setGlobalRedirectList( List<RedirectRule> fullList, List<RedirectRule> globalList )
    {
        List<RedirectRule> list = new LinkedList<RedirectRule>();

        /* Add all of new global redirects to the list */
        for ( RedirectRule item  : globalList ) {
            if ( item.isLocalRedirect()) {
                logger.info( "Local redirect in global list, ignoring" );
                continue;
            }

            list.add( item );
        }

        /* Add all of the local redirects to the new list */
        for ( RedirectRule item : fullList ) {
            if ( item.isLocalRedirect()) list.add( item );
        }

        return list;
    }

    List<RedirectRule> getLocalRedirectList( List<RedirectRule> fullList )
    {
        List<RedirectRule> list = new LinkedList<RedirectRule>();
        
        for ( RedirectRule item : fullList ) {
            if ( item.isLocalRedirect()) list.add( item );
        }
        
        return list;
    }

    List<RedirectRule> setLocalRedirectList( List<RedirectRule> fullList, List<RedirectRule> localList )
    {
        List<RedirectRule> list = new LinkedList<RedirectRule>();

        /* Add all of the global redirects to the new list */
        for ( RedirectRule item : fullList ) {
            if ( !item.isLocalRedirect()) list.add( item );
        }

        /* Add all of local redirects to the end of the list */
        for ( RedirectRule item  : localList ) {
            if ( !item.isLocalRedirect()) {
                logger.info( "Global redirect in local list, ignoring" );
                continue;
            }
            
            /* Set all of the unset properties */
            IntfDBMatcher intfAllMatcher = IntfMatcherFactory.getInstance().getAllMatcher();

            item.setSrcIntf( intfAllMatcher );
            item.setDstIntf( intfAllMatcher );
            
            item.setSrcAddress( IPMatcherFactory.getInstance().getAllMatcher());
            item.setSrcPort( PortMatcherFactory.getInstance().getAllMatcher());
            
            list.add( item );
        }

        return list;        
    }

    List<IPDBMatcher> getEmptyLocalMatcherList()
    {
        return LOCAL_MATCHER_LIST;
    }

    public static RouterUtil getInstance()
    {
        return INSTANCE;
    }

    static
    {
        IPaddr dmz;
        IPaddr setupInternalAddress;
        IPaddr setupInternalSubnet;
        IPaddr setupDhcpStart;
        IPaddr setupDhcpEnd;

        try {
            dmz        = IPaddr.parse( "192.168.1.2" );
            
            setupInternalAddress = IPaddr.parse( "192.168.1.81" );
            setupInternalSubnet = IPaddr.parse( "255.255.255.240" );
            setupDhcpStart = IPaddr.parse( "192.168.1.82" );
            setupDhcpEnd = IPaddr.parse( "192.168.1.94" );
        } catch( Exception e ) {
            System.err.println( "Unable to initialize one of the ip addrs" );
            e.printStackTrace();
            dmz = null;
            setupInternalAddress = null;
            setupInternalSubnet = null;
            setupDhcpStart = null;
            setupDhcpEnd = null;
        }
        
        DEFAULT_DMZ_ADDRESS = dmz;

        SETUP_INTERNAL_ADDRESS = setupInternalAddress;
        SETUP_INTERNAL_SUBNET  = setupInternalSubnet;
        SETUP_DHCP_START       = setupDhcpStart;
        SETUP_DHCP_END         = setupDhcpEnd;

        /* Setup the default list of local matchers, perhaps this should be in the IPMatcherFactory? */
        List<IPDBMatcher> list = new LinkedList<IPDBMatcher>();
        
        IPMatcherFactory ipmf = IPMatcherFactory.getInstance();

        /* always add the local and the all public matcher. */
        list.add( ipmf.getLocalMatcher());
        list.add( ipmf.getAllPublicMatcher());
        
        LOCAL_MATCHER_LIST = Collections.unmodifiableList( list );
    }
}
