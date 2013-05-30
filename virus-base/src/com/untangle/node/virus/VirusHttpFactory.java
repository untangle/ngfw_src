/**
 * $Id$
 */
package com.untangle.node.virus;

import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Factory for HTTP <code>TokenHandler</code>s.
 */
public class VirusHttpFactory implements TokenHandlerFactory
{
    private final VirusNodeImpl node;

    VirusHttpFactory(VirusNodeImpl node)
    {
        this.node = node;
    }

    public TokenHandler tokenHandler(NodeTCPSession session)
    {
        return new VirusHttpHandler(session, node);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
