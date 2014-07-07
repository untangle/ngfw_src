/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

/**
 * New TCP session request event
 */
@SuppressWarnings("serial")
public class TCPNewSessionRequestEvent
{
    private TCPNewSessionRequest sessionRequest;
    private PipelineConnector pipelineConnector;
    
    public TCPNewSessionRequestEvent(PipelineConnector pipelineConnector, TCPNewSessionRequest sessionRequest)
    {
        this.pipelineConnector = pipelineConnector;
        this.sessionRequest = sessionRequest;
    }

    public PipelineConnector pipelineConnector()
    {
        return pipelineConnector;
    }

    public TCPNewSessionRequest sessionRequest()
    {
        return sessionRequest;
    }

}
