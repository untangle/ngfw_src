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

package com.untangle.jnetcap;

import java.net.InetAddress;

public final class ShieldNodeSettings
{
    private final InetAddress address;
    private final InetAddress netmask;
    private final double      divider;

    public ShieldNodeSettings( double divider, InetAddress address, InetAddress netmask )
    {
        this.divider = divider;
        this.address = address;
        this.netmask = netmask;
    }

    public double divider()
    {
        return divider;
    }

    public InetAddress address()
    {
        return address;
    }

    public long addressLong()
    {
        return Inet4AddressConverter.toLong( address );
    }

    public InetAddress netmask()
    {
        return netmask;
    }

    public long netmaskLong()
    {
        return Inet4AddressConverter.toLong( netmask );
    }

}
