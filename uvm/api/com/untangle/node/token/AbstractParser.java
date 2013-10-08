/**
 * $Id: AbstractParser.java 34494 2013-04-06 23:29:09Z mahotz $
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPChunkResult;

/**
 * Abstract base class for parsers.
 */
public abstract class AbstractParser implements Parser
{
    private final String idStr;

    protected final NodeTCPSession session;
    protected final boolean clientSide;

    protected AbstractParser(NodeTCPSession session, boolean clientSide)
    {
        this.session = session;
        this.clientSide = clientSide;

        String name = getClass().getName();

        this.idStr = name + "<" + (clientSide ? "CS" : "SS") + ":"
            + session.id() + ">";
    }

    // Parser methods ---------------------------------------------------------

    /*
     * CasingAdaptor will call the ParseResult/ByteBuffer version
     * CasingCoupler will call the TCPChunkResult/TCPChunkEvent version
     * If you forget to override one or the other in your casing
     * then you will see one of the helpful exception messages
     */

    public ParseResult parse(ByteBuffer chunk) throws ParseException
    {
        throw new ParseException("Unexpected call to base class parse(ByteBuffer)");
    }

    public TCPChunkResult parse(TCPChunkEvent event) throws ParseException
    {
        throw new ParseException("Unexpected call to base class parse(TCPChunkEvent)");
    }

    // Parser noops -----------------------------------------------------------

    public TokenStreamer endSession()
    {
        return null;
    }

    public ParseResult parseEnd(ByteBuffer chunk) throws ParseException
    {
        return null;
    }

    // session manipulation ---------------------------------------------------

    protected void lineBuffering(boolean oneLine)
    {
        if (clientSide)
            {
            session.clientLineBuffering(oneLine);
        } else {
            session.serverLineBuffering(oneLine);
        }
    }

    protected int readLimit()
    {
        if (clientSide) {
            return session.clientReadLimit();
        } else {
            return session.serverReadLimit();
        }
    }

    protected void readLimit(int limit)
    {
        if (clientSide) {
            session.clientReadLimit(limit);
        } else {
            session.serverReadLimit(limit);
        }
    }

    protected void scheduleTimer(long delay)
    {
        session.scheduleTimer(delay);
    }

    protected void cancelTimer()
    {
        session.cancelTimer();
    }

    protected boolean isClientSide()
    {
        return clientSide;
    }

    protected NodeTCPSession getSession()
    {
        return session;
    }

    // no-ops methods ---------------------------------------------------------

    public void handleTimer() { }
    public void handleFinalized() { }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return idStr;
    }
}
