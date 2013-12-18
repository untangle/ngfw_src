/**
 * $Id: CaptureHttpsHandler.java,v 1.00 2012/08/20 11:06:59 dmorris Exp $
 */

package com.untangle.node.capture;

import java.net.InetAddress;

import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.IPDataResult;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.UvmContextFactory;
import org.apache.log4j.Logger;

public class CaptureHttpsHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private CaptureNodeImpl node;

    public CaptureHttpsHandler(CaptureNodeImpl node)
    {
        super(node);
        this.node = node;
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        TCPNewSessionRequest sessreq = event.sessionRequest();

        // look for our special attachment
        InetAddress special = (InetAddress) sessreq.globalAttachment(NodeSession.KEY_CAPTURE_REDIRECT);

        // if attachment not found we just release the session
        if (special == null) {
            sessreq.release();
            return;
        }

        logger.debug("Doing HTTPS-->HTTP redirect for " + special.getHostAddress().toString());

        // first we remove the attachment
        sessreq.globalAttach(NodeSession.KEY_CAPTURE_REDIRECT, null);

        CaptureSSLEngine engine = new CaptureSSLEngine(node.getNodeSettings().getId().toString());
        sessreq.globalAttach(NodeSession.KEY_CAPTURE_SSL_ENGINE, engine);
    }

    public IPDataResult handleTCPClientChunk(TCPChunkEvent event)
    {
        // get the SSL engine attached to the session
        CaptureSSLEngine engine = (CaptureSSLEngine) event.session().globalAttachment(NodeSession.KEY_CAPTURE_SSL_ENGINE);
        return (engine.handleClientData(event.session(), event.data()));
    }
}
