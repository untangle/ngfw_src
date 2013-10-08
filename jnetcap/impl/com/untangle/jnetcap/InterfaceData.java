/*
 * $HeadURL: svn://chef/work/src/jnetcap/impl/com/untangle/jnetcap/InterfaceData.java $
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

package com.untangle.jnetcap;

import java.net.InetAddress;

public final class InterfaceData
{
    private final InetAddress address;
    private final InetAddress netmask;
    private final InetAddress broadcast;

    InterfaceData( long address, long netmask, long broadcast )
    {
        this.address   = Inet4AddressConverter.toAddress( address );
        this.netmask   = Inet4AddressConverter.toAddress( netmask );
        this.broadcast = Inet4AddressConverter.toAddress( broadcast );
    }

    InterfaceData( InetAddress address, InetAddress netmask, InetAddress broadcast )
    {
        this.address   = address;
        this.netmask   = netmask;
        this.broadcast = broadcast;
    }

    public InetAddress getAddress()
    {
        return this.address;
    }

    public InetAddress getNetmask()
    {
        return this.netmask;
    }

    public InetAddress getBroadcast()
    {
        return this.broadcast;
    }
}
