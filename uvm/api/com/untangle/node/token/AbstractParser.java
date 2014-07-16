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

    // Parser methods ---------------------------------------------------------

    /*
     * CasingAdaptor will call the ParseResult/ByteBuffer version
     * CasingCoupler will call the TCPChunkResult/TCPChunkEvent version
     * If you forget to override one or the other in your casing
     * then you will see one of the helpful exception messages
     */

    public ParseResult parse( NodeTCPSession session, ByteBuffer chunk ) throws ParseException
    {
        throw new ParseException("Unexpected call to base class parse(ByteBuffer)");
    }

    public void parseFIXME( NodeTCPSession session, ByteBuffer data ) throws ParseException
    {
        throw new ParseException("Unexpected call to base class parse(TCPChunkEvent)");
    }

    // Parser noops -----------------------------------------------------------

    public TokenStreamer endSession( NodeTCPSession session )
    {
        return null;
    }

    public ParseResult parseEnd( NodeTCPSession session, ByteBuffer chunk ) throws ParseException
    {
        return null;
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
