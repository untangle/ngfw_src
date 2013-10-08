/**
 * $Id: TCPNewSessionRequestEvent.java 34443 2013-04-01 22:53:15Z dmorris $
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

/**
 * New TCP session request event
 */
@SuppressWarnings("serial")
public class TCPNewSessionRequestEvent extends PipelineConnectorEvent
{
    
    public TCPNewSessionRequestEvent(PipelineConnector pipelineConnector, TCPNewSessionRequest sessionRequest)
    {
        super(pipelineConnector, sessionRequest);
    }

    public TCPNewSessionRequest sessionRequest()
    {
        return (TCPNewSessionRequest)getSource();
    }

}
