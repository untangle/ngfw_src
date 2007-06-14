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

import com.untangle.jnetcap.*;

public class UDPPacketCrumb extends PacketCrumb
{
    /**
     * Create a new UDP Packet Crumb.</p>
     *
     * @param ttl     - Time To Live for the packet.
     * @param tos     - Type of service for the packet.
     * @param options - Type of service for the packet.          
     * @param data    - Byte array containing the data.
     * @param offset  - Offset in the byte array.
     * @param limit   - Limit of the data.
     */
    public UDPPacketCrumb( byte ttl, byte tos, byte options[], byte[] data, int offset, int limit )
    {
        super( ttl, tos, options, data, offset, limit );
    }

    /**
     * Create a new UDP Packet Crumb.</p>
     *
     * @param packet  - Packet to base this crumb on.
     * @param data    - Byte array containing the data.
     * @param offset  - Offset in the byte array.
     * @param limit   - Limit of the data.
     */
    public UDPPacketCrumb( UDPPacket packet, byte[] data, int offset, int limit )
    {
        super( packet, data, offset, limit );
    }

    /**
     * Create a new UDP Packet Crumb.</p>
     *
     * @param packet  - Packet to base this crumb on.
     * @param data    - Byte array containing the data.
     * @param limit   - Limit of the data.
     */
    protected UDPPacketCrumb( UDPPacket packet, byte[] data, int limit )
    {
        super( packet, data, limit );
    }

    /**
     * Create a new UDP Packet Crumb.</p>
     *
     * @param packet  - Packet to base this crumb on.
     * @param data    - Byte array containing the data.
     */
    protected UDPPacketCrumb( UDPPacket packet, byte[] data )
    {
        super( packet, data );
    }

    public int type()
    {
        return TYPE_UDP_PACKET;
    }

    public void raze()
    {
        /* XXX What should go in here, C structure is freed automatically */
    }
}
