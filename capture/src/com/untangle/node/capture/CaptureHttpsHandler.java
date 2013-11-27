/**
 * $Id: CaptureHttpsHandler.java,v 1.00 2012/08/20 11:06:59 dmorris Exp $
 */

package com.untangle.node.capture;

import java.net.InetAddress;

import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
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

        // since we found the attachment we're dealing with an HTTPS session
        // that needs to be captured.  To make this happen we tweak the
        // destination server and port so the client instead goes to our
        // internal Apache on a special port which uses mod_rewrite to
        // redirect the original request to the captive page

        try {
            InetAddress addr = UvmContextFactory.context().networkManager().getInterfaceHttpAddress(sessreq.getClientIntf());
            long myport = (8500 + node.getNodeSettings().getId());

            sessreq.setServerAddr(addr);
            sessreq.setServerPort((int) myport);
            return;
        }

        catch (Exception exn) {
            logger.warn("Exception creating HTTPS redirect", exn);
        }
    }
}
