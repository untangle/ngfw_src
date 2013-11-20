/**
 * $Id$
 */
package com.untangle.node.webfilter;

import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Factory for creating <code>WebFilterHandler</code>s.
 *
 */
public class WebFilterFactory implements TokenHandlerFactory
{
    protected final WebFilterBase node;

    // constructors -----------------------------------------------------------

    protected WebFilterFactory(WebFilterBase node)
    {
        this.node = node;
    }

    // TokenHandlerFactory methods --------------------------------------------

    public boolean isTokenSession(NodeTCPSession se)
    {
        return true;
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }

    public TokenHandler tokenHandler(NodeTCPSession session)
    {
        return new WebFilterHandler(session, node);
    }
}
