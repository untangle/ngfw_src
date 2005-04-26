/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.jvector;

import com.metavize.jnetcap.*;


/**
 * ICMPPacketCrumb:
 * A crumb representing ICMP data.
 * When using an ICMP crumb, the data almost never needs to be modified.  The addresses
 * and ports inside of the packet are automatically updated at the endpoints to contain
 * the proper values.  This occurs when writing the crumb to the endpoint.
 */
public class ICMPPacketCrumb extends PacketCrumb
{
    static final byte ICMP_TYPE_MIN = 0;
    static final byte ICMP_TYPE_MAX = 17;

    protected byte icmpType;
    protected byte icmpCode;

    /**
     * Create a new UDP Packet Crumb.</p>
     *
     * @param ttl      - Time To Live for the packet.
     * @param tos      - Type of service for the packet.
     * @param options  - Type of service for the packet.
     * @param icmpType - Type of the ICMP packet.
     * @param icmpCode - Code of the ICMP packet.
     * @param data     - Byte array containing the data.
     * @param offset   - Offset in the byte array.
     * @param limit    - Limit of the data.
     */
    public ICMPPacketCrumb( byte ttl, byte tos, byte options[], byte icmpType, byte icmpCode, 
                            byte[] data, int offset, int limit )
    {
        super( ttl, tos, options, data, offset, limit );
        
        this.icmpType = icmpType;
        this.icmpCode = icmpCode;
    }

    /**
     * Create a new ICMP Packet Crumb.</p>
     *
     * @param packet  - Packet to base this crumb on.
     * @param data    - Byte array containing the data.
     * @param offset  - Offset in the byte array.
     * @param limit   - Limit of the data.
     */
    public ICMPPacketCrumb( ICMPPacket packet, byte[] data, int offset, int limit )
    {        
        /* XXX Fix the options */
        this( packet.traffic().ttl(), packet.traffic().tos(), null, 
              packet.icmpType(), packet.icmpCode(), data, offset, limit );              
    }

    /**
     * Create a new ICMP Packet Crumb.</p>
     *
     * @param packet  - Packet to base this crumb on.
     * @param data    - Byte array containing the data.
     * @param limit   - Limit of the data.
     */
    protected ICMPPacketCrumb( ICMPPacket packet, byte[] data, int limit )
    {
        this( packet, data, 0, limit );
    }

    /**
     * Create a new ICMP Packet Crumb.</p>
     *
     * @param packet  - Packet to base this crumb on.
     * @param data    - Byte array containing the data.
     */
    protected ICMPPacketCrumb( ICMPPacket packet, byte[] data )
    {
        this( packet, data, 0, data.length );
    }

    public int type()
    {
        return TYPE_ICMP_PACKET;
    }

    public byte icmpType()
    {
        return icmpType;
    }

    public void icmpType( byte icmpType )
    {
        if ( icmpType < ICMP_TYPE_MIN || icmpType > ICMP_TYPE_MAX ) {
            throw new IllegalArgumentException( "Invalid ICMP type: " + icmpType );
        }

        this.icmpType = icmpType;
    }

    public byte icmpCode()
    {
        return icmpCode;
    }
    
    public void icmpCode( byte icmpCode )
    {
        /* XXX Probably should do some validation */
        this.icmpCode = icmpCode;
    }

    public void raze()
    {
        /* XXX What should go in here, C structure is freed automatically */
    }
}
