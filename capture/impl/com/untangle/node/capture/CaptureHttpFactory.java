/**
 * $Id: CaptureHttpFactory.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
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
        logger.debug("CaptureHttpFactory");
        this.node = node;
    }

    public TokenHandler tokenHandler(NodeTCPSession session)
    {
        logger.debug("tokenHandler");
        return new CaptureHttpHandler(session, node);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
        logger.debug("handleNewSessionRequest");
    }
}
