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

    // Unparser methods -------------------------------------------------------

    /*
     * CasingCoupler will call the TCPChunkResult/TCPChunkEvent version
     * If you forget to override one or the other in your casing
     * then you will see one of the helpful exception messages
     */

    public void unparse( NodeTCPSession session, ByteBuffer data ) throws UnparseException
    {
        throw new UnparseException("Unexpected call to base class unparse(Token)");
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
