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

