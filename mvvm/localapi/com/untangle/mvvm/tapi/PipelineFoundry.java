/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.tapi;

import java.net.InetSocketAddress;

import com.untangle.mvvm.api.IPSessionDesc;
import com.untangle.mvvm.policy.Policy;
import com.untangle.mvvm.tran.PipelineEndpoints;

/**
 * Compiles pipes based on subscriptions and interest sets.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
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
