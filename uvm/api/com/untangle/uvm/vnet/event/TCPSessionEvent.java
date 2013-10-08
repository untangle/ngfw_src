/**
 * $Id: TCPSessionEvent.java 34443 2013-04-01 22:53:15Z dmorris $
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Base class for all TCP live session events
 */
@SuppressWarnings("serial")
public class TCPSessionEvent extends IPSessionEvent
{
    public TCPSessionEvent(PipelineConnector pipelineConnector, NodeTCPSession session)
    {
        super(pipelineConnector, session);
    }

    public NodeTCPSession session()
    {
        return (NodeTCPSession)getSource();
    }
}
