/**
 * $Id$
 */

package com.untangle.node.capture;

import org.apache.log4j.Logger;

import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;

public class CaptureHttpFactory implements TokenHandlerFactory
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptureNodeImpl node;

    CaptureHttpFactory(CaptureNodeImpl node)
    {
        this.node = node;
    }

    public TokenHandler tokenHandler(NodeTCPSession session)
    {
        return new CaptureHttpHandler(session, node);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
