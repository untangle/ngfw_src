/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * TCP data event -- chunk of bytes received.
 */
@SuppressWarnings("serial")
public class TCPChunkEvent
{
    private ByteBuffer readBuffer;
    private PipelineConnector pipelineConnector;
    private NodeTCPSession session;

    public TCPChunkEvent( PipelineConnector pipelineConnector, NodeTCPSession session, ByteBuffer readBuffer )
    {
        this.pipelineConnector = pipelineConnector;
        this.session = session;
        this.readBuffer = readBuffer;
    }

    public ByteBuffer chunk()
    {
        return readBuffer;
    }

    public ByteBuffer data()
    {
        return readBuffer;
    }

    public NodeTCPSession session()
    {
        return session;
    }

    public PipelineConnector pipelineConnector()
    {
        return pipelineConnector;
    }
}
