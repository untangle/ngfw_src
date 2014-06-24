/**
 * $Id$
 */
package com.untangle.node.token;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Abstract base class for <code>TokenHandler</code>s.
 */
public abstract class AbstractTokenHandler implements TokenHandler
{
    private final NodeTCPSession session;

    protected AbstractTokenHandler(NodeTCPSession session)
    {
        this.session = session;
    }

    public void handleTimer() throws TokenException
    {
        // do nothing
    }

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

    public void handleFinalized() throws TokenException
    {
        // do nothing
    }

    protected NodeTCPSession getSession()
    {
        return session;
    }
}
