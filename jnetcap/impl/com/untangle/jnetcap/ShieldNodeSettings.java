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
