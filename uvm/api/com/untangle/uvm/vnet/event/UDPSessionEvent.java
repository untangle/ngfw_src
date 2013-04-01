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
public class UDPSessionEvent extends IPSessionEvent
{
    
    public UDPSessionEvent(PipelineConnector pipelineConnector, NodeUDPSession session)
    {
        super(pipelineConnector, session);
    }

    public NodeUDPSession session()
    {
        return (NodeUDPSession)getSource();
    }
}
