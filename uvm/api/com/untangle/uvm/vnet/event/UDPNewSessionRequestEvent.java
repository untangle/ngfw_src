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
public class UDPNewSessionRequestEvent extends PipelineConnectorEvent
{
    public UDPNewSessionRequestEvent(PipelineConnector pipelineConnector, UDPNewSessionRequest sessionRequest)
    {
        super(pipelineConnector, sessionRequest);
    }

    public UDPNewSessionRequest sessionRequest()
    {
        return (UDPNewSessionRequest)getSource();
    }
}
