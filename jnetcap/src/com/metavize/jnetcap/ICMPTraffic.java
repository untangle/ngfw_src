/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: NetcapUDPSession.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.jnetcap;

public class ICMPTraffic extends IPTraffic
{
    /* XXX MAKE SURE THESE CODES DO NOT CONFLICT WITH THOSE IN IPTraffic */
    private static final int FLAG_TYPE      = 0x080;
    private static final int FLAG_CODE      = 0x081;
    
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
}

