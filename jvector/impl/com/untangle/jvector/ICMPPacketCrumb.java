/*
 * $HeadURL:$
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

package com.untangle.jvector;

import java.net.InetAddress;

import com.untangle.jnetcap.*;

/**
 * ICMPPacketCrumb:
 * A crumb representing ICMP data.
 * When using an ICMP crumb, the data almost never needs to be modified.  The addresses
 * and ports inside of the error packet are automatically updated at the endpoints to contain
 * the proper values.  This occurs when writing the crumb to the endpoint.
 */
public class ICMPPacketCrumb extends PacketCrumb
{
    static final byte ICMP_TYPE_MIN = 0;
    static final byte ICMP_TYPE_MAX = 17;

    protected byte icmpType;
    protected byte icmpCode;

    /**
     * The source address of the packet.  If null, this will be set to the source for the session.
     * This is mainly used for traceroute which has different source addresses for each packet.
     */
    protected InetAddress source;

    /**
     * Create a new UDP Packet Crumb.</p>
     *
     * @param ttl      - Time To Live for the packet.
     * @param tos      - Type of service for the packet.
     * @param options  - Type of service for the packet.
     * @param icmpType - Type of the ICMP packet.
     * @param icmpCode - Code of the ICMP packet.
     * @param source   - Source address of the packet.
     * @param data     - Byte array containing the data.
     * @param offset   - Offset in the byte array.
     * @param limit    - Limit of the data.
     */
    public ICMPPacketCrumb( byte ttl, byte tos, byte options[], byte icmpType, byte icmpCode,
                            InetAddress source, byte[] data, int offset, int limit )
    {
        super( ttl, tos, options, data, offset, limit );

        this.source   = source;
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
              packet.icmpType(), packet.icmpCode(), packet.icmpSource( data, limit ),
              data, offset, limit );
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

    public void icmpType( byte icmpType ) throws JVectorException
    {
        if ( icmpType < ICMP_TYPE_MIN || icmpType > ICMP_TYPE_MAX ) {
            throw new JVectorException( "Invalid ICMP type: " + icmpType );
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

    public InetAddress source()
    {
        return this.source;
    }

    public void source( InetAddress source )
    {
        this.source = source;
    }


    /**
     * Repair the TCP/UDP/IP Header inside of the data block of an ICMP error packet so that it
     * contains the correct information.  If this is not an error packet, this does nothing.
     * This should only eb
     */
    int updatePacket(  int icmpId, ICMPMailbox icmpMailbox )
    {
        return Netcap.updateIcmpPacket( this.data, this.limit, this.icmpType, this.icmpCode,
                                        icmpId, icmpMailbox );
    }

    public void raze()
    {
        /* XXX What should go in here, C structure is freed automatically */
    }
}
