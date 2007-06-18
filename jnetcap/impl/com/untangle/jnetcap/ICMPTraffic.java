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

public class ICMPTraffic extends IPTraffic
{
    /* XXX MAKE SURE THESE CODES DO NOT CONFLICT WITH THOSE IN IPTraffic */
    private static final int FLAG_TYPE = 0x080;
    private static final int FLAG_CODE = 0x081;

    private static final long SAME_SOURCE = -1L;
    
    ICMPTraffic( CPointer pointer )
    {
        super( pointer );
    }

    /**
     * Retrieve the ICMP type from the packet
     */
    public byte icmpType()
    {
        return (byte)getIntValue( FLAG_TYPE );
    }

    /**
     * Retrieve the ICMP code from the packet
     */
    public byte icmpCode()
    {
        return (byte)getIntValue( FLAG_CODE );
    }

    public InetAddress icmpSource( byte data[], int limit )
    {
        long source = icmpSource( pointer.value(), data, limit );
        
        if ( source == SAME_SOURCE ) return null;
        
        return Inet4AddressConverter.toAddress( source );
    }

    private native long icmpSource( long packetPointer, byte[] data, int limit );
}

