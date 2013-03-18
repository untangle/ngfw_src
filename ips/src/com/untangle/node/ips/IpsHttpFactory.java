/**
 * $Id$
 */
package com.untangle.node.ips;

import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;

public class IpsHttpFactory implements TokenHandlerFactory
{
    private final IpsNodeImpl node;

    IpsHttpFactory(IpsNodeImpl node)
    {
        this.node = node;
    }

    public TokenHandler tokenHandler(NodeTCPSession session)
    {
        return new IpsHttpHandler(session, node);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}

