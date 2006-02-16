/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.nat;

import com.metavize.mvvm.tran.IPaddr;

class NatUtil
{
    static final IPaddr DEFAULT_NAT_ADDRESS;
    static final IPaddr DEFAULT_NAT_NETMASK;
    
    static final IPaddr DEFAULT_DMZ_ADDRESS;
    
    static final IPaddr DEFAULT_DHCP_START;
    static final IPaddr DEFAULT_DHCP_END;

    /* Four hours, this parameter is actually unused */
    static final int DEFAULT_LEASE_TIME_SEC = 4 * 60 * 60;
    
    static
    {
        IPaddr natAddress, natNetmask, dmz, dhcpStart, dhcpEnd;

        try {
            natAddress = IPaddr.parse( "192.168.1.1" );
            natNetmask = IPaddr.parse( "255.255.255.0" );
            dmz        = IPaddr.parse( "192.168.1.2" );
            dhcpStart  = IPaddr.parse( "192.168.1.100" );
            dhcpEnd    = IPaddr.parse( "192.168.1.200" );
        } catch( Exception e ) {
            System.err.println( "Unable to initialize one of the ip addrs" );
            e.printStackTrace();
            natAddress = natNetmask = dmz = dhcpStart = dhcpEnd = null;
        }
        
        DEFAULT_NAT_ADDRESS = natAddress;
        DEFAULT_NAT_NETMASK = natNetmask;
        DEFAULT_DMZ_ADDRESS = dmz;
        DEFAULT_DHCP_START  = dhcpStart;
        DEFAULT_DHCP_END    = dhcpEnd;
    }
}
