/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.net.InetSocketAddress;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * Compiles pipes based on subscriptions and interest sets.
 */
public interface PipelineFoundry
{
    PipelineConnector createPipelineConnector( PipeSpec spec, SessionEventListener listener, Fitting inputFitting, Fitting outputFitting );

    void registerPipelineConnector( PipelineConnector pipelineConnector );

    void deregisterPipelineConnector( PipelineConnector pipelineConnector );

    void registerCasing( PipelineConnector insidePipelineConnector, PipelineConnector outsidePipelineConnector );

    void deregisterCasing( PipelineConnector insidePipelineConnector);

    /* Remove all of the cached chains */
    void clearCache();

    void addConnectionFittingHint( InetSocketAddress socketAddress, Fitting fitting );

    Pipeline getPipeline( long sessionId );
}
