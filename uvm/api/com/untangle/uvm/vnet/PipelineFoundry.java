/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.net.InetSocketAddress;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.SessionEventHandler;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.app.App;

import org.json.JSONArray;

/**
 * Compiles pipes based on subscriptions and interest sets.
 */
public interface PipelineFoundry
{
    void registerPipelineConnector( PipelineConnector pipelineConnector );

    void deregisterPipelineConnector( PipelineConnector pipelineConnector );

    void clearCache();     /* Remove all of the cached chains */

    void addConnectionFittingHint( InetSocketAddress socketAddress, Fitting fitting );

    PipelineConnector create( String name, App app, Subscription subscription, SessionEventHandler listener, Fitting inputFitting, Fitting outputFitting, Affinity affinity, Integer affinityStrength, boolean premium );

    PipelineConnector create( String name, App app, Subscription subscription, SessionEventHandler listener, Fitting inputFitting, Fitting outputFitting, Affinity affinity, Integer affinityStrength, boolean premium, String buddy );

    JSONArray getPipelineOrder( Integer policyId, short protocol, String clientIp, String serverIp, int clientPort, int serverPort);
}
