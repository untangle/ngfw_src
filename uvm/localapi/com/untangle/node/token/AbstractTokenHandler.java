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

package com.untangle.node.token;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.tapi.Pipeline;
import com.untangle.uvm.tapi.TCPSession;
import org.apache.log4j.Logger;

public abstract class AbstractTokenHandler implements TokenHandler
{
    private final Logger logger = Logger.getLogger(AbstractTokenHandler.class);

    private final TCPSession session;
    private final Pipeline pipeline;

    // constructors -----------------------------------------------------------

    protected AbstractTokenHandler(TCPSession session)
    {
        this.session = session;
        this.pipeline = UvmContextFactory.context().pipelineFoundry()
            .getPipeline(session.id());
    }

    // TokenHandler methods ---------------------------------------------------

    public void handleTimer() throws TokenException { }

    public void handleClientFin() throws TokenException
    {
        session.shutdownServer();
    }

    public void handleServerFin() throws TokenException
    {
        session.shutdownClient();
    }

    public TokenResult releaseFlush()
    {
        return TokenResult.NONE;
    }

    public void handleFinalized() throws TokenException { }

    // protected methods ------------------------------------------------------

    protected TCPSession getSession()
    {
        return session;
    }

    protected Pipeline getPipeline()
    {
        return pipeline;
    }
}
