/**
 * $Id$
 */
package com.untangle.node.spyware;

import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;

class SpywareHttpFactory implements TokenHandlerFactory
{
    private final SpywareImpl node;

    // constructors -----------------------------------------------------------

    SpywareHttpFactory(SpywareImpl node)
    {
        this.node = node;
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(NodeTCPSession session)
    {
        return new SpywareHttpHandler(session, node);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
