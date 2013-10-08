/**
 * $Id: PipelineConnectorEvent.java 34443 2013-04-01 22:53:15Z dmorris $
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.PipelineConnector;

/**
 * Top level abstract class for all VNet events
 */
@SuppressWarnings("serial")
public abstract class PipelineConnectorEvent extends java.util.EventObject
{
    private PipelineConnector pipelineConnector;

    protected PipelineConnectorEvent(PipelineConnector pipelineConnector, Object source)
    {
        super(source);
        this.pipelineConnector = pipelineConnector;
    }

    public PipelineConnector pipelineConnector()
    {
        return pipelineConnector;
    }
}
