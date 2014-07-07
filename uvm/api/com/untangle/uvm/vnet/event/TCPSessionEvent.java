/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Base class for all TCP live session events
 */
@SuppressWarnings("serial")
public class TCPSessionEvent
{
    private PipelineConnector pipelineConnector;
    private NodeTCPSession session;

    public TCPSessionEvent(PipelineConnector pipelineConnector, NodeTCPSession session)
    {
        this.pipelineConnector = pipelineConnector;
        this.session = session;
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
