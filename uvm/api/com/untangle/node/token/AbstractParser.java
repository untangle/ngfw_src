/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Abstract base class for parsers.
 */
public abstract class AbstractParser implements Parser
{
    protected final boolean clientSide;

    protected AbstractParser( boolean clientSide )
    {
        this.clientSide = clientSide;
    }

    public void parse( NodeTCPSession session, ByteBuffer chunk )
    {
        throw new RuntimeException("Unexpected call to base class parse(ByteBuffer)");
    }

    public void endSession( NodeTCPSession session )
    {
        if ( clientSide )
            session.shutdownServer();
        else
            session.shutdownClient();
    }

    public void parseEnd( NodeTCPSession session, ByteBuffer chunk )
    {
        return;
    }

    // session manipulation ---------------------------------------------------

    protected void lineBuffering( NodeTCPSession session, boolean oneLine )
    {
        if (clientSide)
            {
            session.clientLineBuffering(oneLine);
        } else {
            session.serverLineBuffering(oneLine);
        }
    }

    protected long readLimit( NodeTCPSession session )
    {
        if (clientSide) {
            return session.clientReadLimit();
        } else {
            return session.serverReadLimit();
        }
    }

    protected void readLimit( NodeTCPSession session, long limit )
    {
        if (clientSide) {
            session.clientReadLimit(limit);
        } else {
            session.serverReadLimit(limit);
        }
    }

    protected void scheduleTimer( NodeTCPSession session, long delay )
    {
        session.scheduleTimer(delay);
    }

    protected void cancelTimer( NodeTCPSession session )
    {
        session.cancelTimer();
    }

    protected boolean isClientSide()
    {
        return clientSide;
    }

    // no-ops methods ---------------------------------------------------------

    public void handleTimer( NodeSession session ) { }
    public void handleFinalized( NodeTCPSession session ) { }
}
