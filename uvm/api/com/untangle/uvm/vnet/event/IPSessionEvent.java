/**
 * $Id: IPSessionEvent.java 34443 2013-04-01 22:53:15Z dmorris $
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * Base class for all IP live session events
 */
@SuppressWarnings("serial")
public class IPSessionEvent extends SessionEvent
{

    public IPSessionEvent(PipelineConnector pipelineConnector, NodeSession session)
    {
        super(pipelineConnector, session);
    }

    public NodeSession ipsession()
    {
        return (NodeSession)getSource();
    }
}
