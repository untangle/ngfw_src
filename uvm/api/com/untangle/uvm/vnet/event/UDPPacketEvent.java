/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.NodeUDPSession;

/**
 * UDP packet event -- packet received.
 */
@SuppressWarnings("serial")
public class UDPPacketEvent extends UDPSessionEvent implements IPDataEvent
{
    private ByteBuffer packetBuffer;
    private IPPacketHeader header;

    public UDPPacketEvent( ArgonConnector argonConnector, NodeUDPSession session, ByteBuffer packetBuffer, IPPacketHeader header )
    {
        super(argonConnector, session);
        this.header = header;
        this.packetBuffer = packetBuffer;
    }

    public ByteBuffer packet()
    {
        return packetBuffer;
    }

    public ByteBuffer data()
    {
        return packetBuffer;
    }

    public IPPacketHeader header()
    {
        return header;
    }
}
