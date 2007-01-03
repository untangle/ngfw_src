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
