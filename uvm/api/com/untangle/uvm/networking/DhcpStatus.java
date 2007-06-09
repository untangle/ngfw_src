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

package com.untangle.uvm.networking;

import java.io.Serializable;

import java.net.Inet4Address;
import java.net.InetAddress;

import com.untangle.uvm.node.IPaddr;

/**
 * <code>DhcpStatus</code> contains the current state of an interface
 * that is configured with DHCP.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class DhcpStatus implements Serializable
{
    /* Just used in the case where there is no status */
    static final DhcpStatus EMPTY_STATUS = new DhcpStatus( null, null, null, null, null );

    /* The address that is presently assigned to an interface */
    private final IPaddr address;

    /* The current netmask of a dynamically configured interface */
    private final IPaddr netmask;

    /* The default route for a dynamically configured interface */
    private final IPaddr defaultRoute;

    /* Prmary DNS server retrieved from DHCP */
    private final IPaddr dns1;

    /* Secondary DNS server retrieved from DHCP */
    private final IPaddr dns2;

    DhcpStatus( IPNetwork network )
    {
        this( network.getNetwork(), network.getNetmask());
    }

    DhcpStatus( IPaddr address, IPaddr netmask )
    {
        this( address, netmask, null, null, null );
    }
    
    DhcpStatus( InetAddress address, InetAddress netmask )
    {
        this( new IPaddr((Inet4Address)address ), new IPaddr((Inet4Address)netmask ), null, null, null );
    }

    DhcpStatus( IPaddr address, IPaddr netmask, IPaddr defaultRoute, IPaddr dns1, IPaddr dns2 )
    {
        this.address      = (( address == null ) ? NetworkUtil.EMPTY_IPADDR : address );
        this.netmask      = (( netmask == null ) ? NetworkUtil.EMPTY_IPADDR : netmask );
        this.defaultRoute = (( defaultRoute == null ) ? NetworkUtil.EMPTY_IPADDR : defaultRoute );
        this.dns1         = (( dns1 == null ) ? NetworkUtil.EMPTY_IPADDR : dns1 );
        this.dns2         = (( dns2 == null ) ? NetworkUtil.EMPTY_IPADDR : dns2 );
    }

    public IPaddr getAddress()
    {
        return this.address;
    }

    public IPaddr getNetmask()
    {
        return this.netmask;
    }

    public IPaddr getDefaultRoute()
    {
        return this.defaultRoute;
    }

    public IPaddr getDns1()
    {
        return this.dns1;
    }

    public boolean hasDns2()
    {
        return this.dns2.isEmpty();
    }

    public IPaddr getDns2()
    {
        return this.dns2;
    }

    public String toString()
    {
        return "Dhcp Status:" + 
            "\nAddress:       " + this.address +
            "\nNetmask:       " + this.netmask +
            "\nDefault Route: " + this.defaultRoute +
            "\nDNS 1:         " + this.dns1 +
            "\nDNS 2:         " + this.dns2;
    }
}
