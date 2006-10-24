/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
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

import com.metavize.mvvm.api.IPSessionDesc;
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
    PipelineEndpoints createInitialEndpoints(IPSessionDesc start);
    void registerEndpoints(PipelineEndpoints pe);
    void destroy(IPSessionDesc start, IPSessionDesc end, PipelineEndpoints pe, String uid);

    void registerMPipe(MPipe mPipe);
    void deregisterMPipe(MPipe mPipe);

    void registerCasing(MPipe insideMPipe, MPipe outsideMPipe);
    void deregisterCasing(MPipe insideMPipe);

    void registerConnection(InetSocketAddress socketAddress, Fitting fitting);
    Pipeline getPipeline(int sessionId);
}
