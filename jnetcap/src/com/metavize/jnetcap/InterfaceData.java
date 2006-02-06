/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.jnetcap;

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

    public String toString()
    {
        return address.getHostAddress() + "/" + netmask.getHostAddress();
    }

    public boolean equals( Object o )
    {
        if (!(o instanceof InterfaceData )) return false;

        InterfaceData id = (InterfaceData)o;
        
        if ((( id.address == null )   ? ( this.address == null )   : id.address.equals( this.address )) &&
            (( id.netmask == null )   ? ( this.netmask == null )   : id.netmask.equals( this.netmask )) &&
            (( id.broadcast == null ) ? ( this.broadcast == null ) : id.broadcast.equals( this.broadcast ))) {
            return true;
        }
        return false;
    }
    
    public int hashCode()
    {
        int hashCode = 17;
        hashCode += ( 37 * hashCode ) + (( this.address == null )   ? 17 : this.address.hashCode());
        hashCode += ( 37 * hashCode ) + (( this.netmask == null )   ? 17 : this.netmask.hashCode());
        hashCode += ( 37 * hashCode ) + (( this.broadcast == null ) ? 17 : this.broadcast.hashCode());

        return hashCode;
    }
}
