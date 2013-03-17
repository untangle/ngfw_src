/**
 * $Id$
 */
package com.untangle.node.router;

import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;

public class RouterFtpFactory implements TokenHandlerFactory
{
    private final RouterImpl node;

    RouterFtpFactory( RouterImpl node )
    {
        this.node = node;
    }

    public TokenHandler tokenHandler( NodeTCPSession session )
    {
        return new RouterFtpHandler( session, node);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
