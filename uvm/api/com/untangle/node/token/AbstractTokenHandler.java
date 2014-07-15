/**
 * $Id$
 */
package com.untangle.node.token;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

/**
 * Abstract base class for <code>TokenHandler</code>s.
 */
public abstract class AbstractTokenHandler implements TokenHandler
{
    protected AbstractTokenHandler() { }

    public void handleNewSessionRequest( TCPNewSessionRequest tsr )
    {
        // do nothing
    }
    
    public void handleNewSession( NodeTCPSession session )
    {
        // do nothing
    }

    public void handleTimer( NodeSession session ) throws TokenException
    {
        // do nothing
    }

    public void handleClientFin( NodeTCPSession session ) throws TokenException
    {
        session.shutdownServer();
    }

    public void handleServerFin( NodeTCPSession session ) throws TokenException
    {
        session.shutdownClient();
    }

    public TokenResult releaseFlush( NodeTCPSession session )
    {
        return TokenResult.NONE;
    }

    public void handleFinalized( NodeTCPSession session ) throws TokenException
    {
        // do nothing
    }
}
