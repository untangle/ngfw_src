/**
 * $Id: UDPPacketCrumb.java 35567 2013-08-08 07:47:12Z dmorris $
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
