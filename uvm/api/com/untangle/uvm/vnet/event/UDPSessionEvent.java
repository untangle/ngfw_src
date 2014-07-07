/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeUDPSession;

/**
 * Base class for all UDP live session events
 */
@SuppressWarnings("serial")
public class UDPSessionEvent
{
    private PipelineConnector pipelineConnector;
    private NodeUDPSession session;
        
    public UDPSessionEvent( PipelineConnector pipelineConnector, NodeUDPSession session )
    {
        this.pipelineConnector = pipelineConnector;
        this.session = session;
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
