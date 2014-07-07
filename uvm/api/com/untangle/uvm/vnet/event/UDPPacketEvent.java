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
public class UDPPacketEvent
{
    private PipelineConnector pipelineConnector;
    private NodeUDPSession session;

    private ByteBuffer packetBuffer;
    private IPPacketHeader header;

    public UDPPacketEvent( PipelineConnector pipelineConnector, NodeUDPSession session, ByteBuffer packetBuffer, IPPacketHeader header )
    {
        this.pipelineConnector = pipelineConnector;
        this.session = session;
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

    public NodeUDPSession session()
    {
        return session;
    }

    public PipelineConnector pipelineConnector()
    {
        return pipelineConnector;
    }
}
