/**
 * $Id: AbstractUnparser.java 34494 2013-04-06 23:29:09Z mahotz $
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.TCPStreamer;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPChunkResult;

/**
 * Abstract base class for <code>Unparser</code>s.
 */
public abstract class AbstractUnparser implements Unparser
{
    private final String idStr;

    protected final NodeTCPSession session;
    protected final boolean clientSide;

    protected AbstractUnparser(NodeTCPSession session, boolean clientSide)
    {
        this.session = session;
        this.clientSide = clientSide;

        String name = getClass().getName();

        this.idStr = name + "<" + (clientSide ? "CS" : "SS") + ":"
            + session.id() + ">";
    }

    // Unparser methods -------------------------------------------------------

    /*
     * CasingAdaptor will call the UnparseResult/Token version
     * CasingCoupler will call the TCPChunkResult/TCPChunkEvent version
     * If you forget to override one or the other in your casing
     * then you will see one of the helpful exception messages
     */

    public UnparseResult unparse(Token token) throws UnparseException
    {
        throw new UnparseException("Unexpected call to base class unparse(ByteBuffer)");
    }

    public TCPChunkResult unparse(TCPChunkEvent event) throws UnparseException
    {
        throw new UnparseException("Unexpected call to base class unparse(Token)");
    }

    public UnparseResult releaseFlush()
    {
        return UnparseResult.NONE;
    }

    public TCPStreamer endSession()
    {
        return null;
    }

    public void handleFinalized()
    {
    }

    // protected methods ------------------------------------------------------

    protected boolean isClientSide()
    {
        return clientSide;
    }

    protected NodeTCPSession getSession()
    {
        return session;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return idStr;
    }
}
