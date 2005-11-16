/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tapi;

import java.net.InetSocketAddress;

import com.metavize.jnetcap.NetcapSession;
import com.metavize.mvvm.argon.IPSessionDesc;
import com.metavize.mvvm.argon.PipelineDesc;
import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.tran.PipelineEndpoints;

/**
 * Compiles pipes based on subscriptions and interest sets.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public interface PipelineFoundry
{
    /**
     * Compiles a pipe from a NetcapSession.
     *
     * @param sessionDesc XXX
     * @return a list of <code>ArgonAgents</code>.
     */
    PipelineDesc weld(IPSessionDesc sessionDesc);

    PipelineEndpoints createInitialEndpoints(IPSessionDesc start);
    void registerEndpoints(PipelineEndpoints pe);
    void destroy(IPSessionDesc start, IPSessionDesc end);

    void registerMPipe(MPipe mPipe);
    void deregisterMPipe(MPipe mPipe);

    void registerCasing(MPipe insideMPipe, MPipe outsideMPipe);
    void deregisterCasing(MPipe insideMPipe);

    void registerConnection(InetSocketAddress socketAddress, Fitting fitting);

    Pipeline getPipeline(int sessionId);
}
