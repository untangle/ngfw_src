/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UDPPacketDesc.java,v 1.1 2005/01/05 06:02:12 rbscott Exp $
 */

package com.metavize.jvector;

import com.metavize.jnetcap.UDPPacket;
import com.metavize.jnetcap.IPTraffic;

/**
 * This class just contains non endpoint related data from a UDP packet such as TTL, TOS and options.
 */
public class UDPPacketDesc
{
    protected byte ttl;
    protected byte tos;
    protected byte options[];
    
    public UDPPacketDesc( byte ttl, byte tos, byte options[] )
    {
        this.ttl = ttl;
        this.tos = tos;
        this.options = options;
    }

    public UDPPacketDesc( IPTraffic traffic )
    {
        this( traffic.ttl(), traffic.tos(), null );
    }

    public UDPPacketDesc( UDPPacket packet )
    {
        this( packet.traffic());
    }

    public byte ttl() 
    { 
        return ttl; 
    }
    
    public byte tos() 
    { 
        return tos;
    }

    public byte[] options()
    {
        return options;
    }

    public void ttl( byte value )
    {
        ttl = value;
    }

    public void tos( byte value )
    {
        tos = value;
    }

    public void options( byte[] value )
    {
        options = value;
    }
}
