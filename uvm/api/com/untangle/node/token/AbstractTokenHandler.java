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

    public void handleTimer( NodeSession session )
    {
        // do nothing
    }

    public void handleClientFin( NodeTCPSession session )
    {
        session.shutdownServer();
    }

    public void handleServerFin( NodeTCPSession session )
    {
        session.shutdownClient();
    }

    public void releaseFlush( NodeTCPSession session )
    {
        return;
    }

    public void handleFinalized( NodeTCPSession session )
    {
        // do nothing
    }
}
