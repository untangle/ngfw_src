/**
 * $Id$
 */
package com.untangle.jvector;

import com.untangle.jnetcap.*;

/**
 * UDPPacketCrumb is a Crumb thats is read from a UDPSource and sent to a UDPSink
 */
public class UDPPacketCrumb extends PacketCrumb
{
    /**
     * Create a new UDP Packet Crumb.
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
     * Create a new UDP Packet Crumb.
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
     * Create a new UDP Packet Crumb.
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
     * Create a new UDP Packet Crumb.
     *
     * @param packet  - Packet to base this crumb on.
     * @param data    - Byte array containing the data.
     */
    protected UDPPacketCrumb( UDPPacket packet, byte[] data )
    {
        super( packet, data );
    }

    /**
     * type - gets the type of crumb
     * @return
     */
    public int type()
    {
        return TYPE_UDP_PACKET;
    }

    /**
     * raze
     */
    public void raze()
    {
        /* Do nothing, C structure is freed automatically */
    }
}
