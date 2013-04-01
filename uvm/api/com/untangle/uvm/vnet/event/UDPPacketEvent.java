/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeUDPSession;

/**
 * UDP packet event -- packet received.
 */
@SuppressWarnings("serial")
public class UDPPacketEvent extends UDPSessionEvent implements IPDataEvent
{
    private ByteBuffer packetBuffer;
    private IPPacketHeader header;

    public UDPPacketEvent( PipelineConnector pipelineConnector, NodeUDPSession session, ByteBuffer packetBuffer, IPPacketHeader header )
    {
        super(pipelineConnector, session);
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
