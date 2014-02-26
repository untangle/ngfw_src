/**
 * $Id$
 */
package com.untangle.node.token;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Abstract base class for <code>TokenHandler</code>s.
 */
public abstract class AbstractTokenHandler implements TokenHandler
{
    private final NodeTCPSession session;
    private final Pipeline pipeline;

    // constructors -----------------------------------------------------------

    protected AbstractTokenHandler(NodeTCPSession session)
    {
        this.session = session;
        this.pipeline = UvmContextFactory.context().pipelineFoundry().getPipeline(session.id());
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

    protected NodeTCPSession getSession()
    {
        return session;
    }

    protected Pipeline getPipeline()
    {
        return pipeline;
    }
}
