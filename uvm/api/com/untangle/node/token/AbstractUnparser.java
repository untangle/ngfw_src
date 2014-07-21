/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Abstract base class for <code>Unparser</code>s.
 */
public abstract class AbstractUnparser implements Unparser
{
    protected final boolean clientSide;

    protected AbstractUnparser( boolean clientSide )
    {
        this.clientSide = clientSide;
    }

    public void unparse( NodeTCPSession session, Token token )
    {
        throw new RuntimeException("Unexpected call to base class unparse(token)");
    }

    public void unparse( NodeTCPSession session, ByteBuffer data )
    {
        throw new RuntimeException("Unexpected call to base class unparse(data)");
    }

    public void releaseFlush( NodeTCPSession session )
    {
        return;
    }

    public void endSession( NodeTCPSession session )
    {
        if ( clientSide )
            session.shutdownClient();
        else
            session.shutdownServer();
    }

    public void handleFinalized( NodeTCPSession session )
    {
        
    }

    // protected methods ------------------------------------------------------

    protected boolean isClientSide()
    {
        return clientSide;
    }
}
