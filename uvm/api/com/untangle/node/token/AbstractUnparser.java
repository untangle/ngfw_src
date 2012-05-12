/**
 * $Id$
 */
package com.untangle.node.token;

import com.untangle.uvm.vnet.NodeTCPSession;

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

    public UnparseResult releaseFlush()
    {
        return UnparseResult.NONE;
    }

    public void handleFinalized() {
        //Noop
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
