/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.net.InetSocketAddress;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.SessionEventHandler;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.node.Node;

/**
 * Compiles pipes based on subscriptions and interest sets.
 */
public interface PipelineFoundry
{
    //PipelineConnector createPipelineConnector( PipeSpec spec, SessionEventHandler listener, Fitting inputFitting, Fitting outputFitting );

    void registerPipelineConnector( PipelineConnector pipelineConnector );

    void deregisterPipelineConnector( PipelineConnector pipelineConnector );

    void registerCasing( PipelineConnector insidePipelineConnector, PipelineConnector outsidePipelineConnector );

    void deregisterCasing( PipelineConnector insidePipelineConnector, PipelineConnector outsidePipelineConnector );

    /* Remove all of the cached chains */
    void clearCache();

    void addConnectionFittingHint( InetSocketAddress socketAddress, Fitting fitting );

    public PipelineConnector create( String name, Node node, Subscription subscription, SessionEventHandler listener, Fitting inputFitting, Fitting outputFitting, Affinity affinity, Integer affinityStrength );
}
