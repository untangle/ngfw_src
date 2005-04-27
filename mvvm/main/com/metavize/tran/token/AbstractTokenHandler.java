/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.TCPSession;
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
        this.pipeline = MvvmContextFactory.context().pipelineFoundry()
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
