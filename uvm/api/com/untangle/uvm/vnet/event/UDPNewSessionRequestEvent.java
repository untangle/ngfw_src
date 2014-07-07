/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

/**
 * New UDP session request event
 */
@SuppressWarnings("serial")
public class UDPNewSessionRequestEvent
{
    private UDPNewSessionRequest sessionRequest;
    private PipelineConnector pipelineConnector;
    
    public UDPNewSessionRequestEvent(PipelineConnector pipelineConnector, UDPNewSessionRequest sessionRequest)
    {
        this.pipelineConnector = pipelineConnector;
        this.sessionRequest = sessionRequest;
    }

    public PipelineConnector pipelineConnector()
    {
        return pipelineConnector;
    }

    public UDPNewSessionRequest sessionRequest()
    {
        return sessionRequest;
    }
}
