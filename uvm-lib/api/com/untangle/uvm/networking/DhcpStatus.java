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
