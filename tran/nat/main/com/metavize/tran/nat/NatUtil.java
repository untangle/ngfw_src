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

import com.metavize.mvvm.networking.NetworkUtil;

class NatUtil
{
    static final IPaddr DEFAULT_NAT_ADDRESS = NetworkUtil.DEFAULT_NAT_ADDRESS;
    static final IPaddr DEFAULT_NAT_NETMASK = NetworkUtil.DEFAULT_NAT_NETMASK;
    
    static final IPaddr DEFAULT_DMZ_ADDRESS;
    
    static final IPaddr DEFAULT_DHCP_START = NetworkUtil.DEFAULT_DHCP_START;
    static final IPaddr DEFAULT_DHCP_END   = NetworkUtil.DEFAULT_DHCP_END;

    /* Four hours, this parameter is actually unused */
    static final int DEFAULT_LEASE_TIME_SEC = NetworkUtil.DEFAULT_LEASE_TIME_SEC;
    
    static
    {
        IPaddr dmz;

        try {
            dmz        = IPaddr.parse( "192.168.1.2" );
        } catch( Exception e ) {
            System.err.println( "Unable to initialize one of the ip addrs" );
            e.printStackTrace();
            natAddress = natNetmask = dmz = null;
        }
        
        DEFAULT_DMZ_ADDRESS = dmz;
    }
}
