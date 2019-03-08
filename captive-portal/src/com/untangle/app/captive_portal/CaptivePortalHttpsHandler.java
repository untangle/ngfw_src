/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.nio.ByteBuffer;
import java.net.InetAddress;

import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import org.apache.log4j.Logger;

/**
 * This is the HTTPS handler used when we are configured to capture and redirect
 * requests from unauthenticated clients. It's not a perfect solution because
 * the client will ALWAYS show some kind of error to the user. It will either be
 * unknown ca because the client isn't configured to trust our CA certificate,
 * or it will be a name error because we intercept a secure connection attempt
 * that we redirect to our capture page.
 * 
 * @author mahotz
 * 
 */

public class CaptivePortalHttpsHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptivePortalApp captureApp;

    /**
     * Our consturctor
     * 
     * @param app
     *        The application instance that created us
     */
    public CaptivePortalHttpsHandler(CaptivePortalApp app)
    {
        super(app);
        this.captureApp = app;
    }

    /**
     * Handle new session requests by looking for the special attachment added
     * by the traffic handler indicating we need to do https-->http redirection.
     * 
     * @param sessreq
     *        The session request
     */
    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequest sessreq)
    {
        // look for our special attachment
        InetAddress special = (InetAddress) sessreq.globalAttachment(AppSession.KEY_CAPTIVE_PORTAL_REDIRECT);

        // if attachment not found we just release the session
        if (special == null) {
            sessreq.release();
            return;
        }

        logger.debug("Performing HTTPS processing for " + special.getHostAddress().toString());

        // first we remove the attachment
        sessreq.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_REDIRECT, null);

        // create the SSL
        CaptivePortalSSLEngine engine = new CaptivePortalSSLEngine(app.getAppSettings().getId().toString(), captureApp);
        sessreq.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_SSL_ENGINE, engine);
    }

    /**
     * Handle chunks of data received from the client
     * 
     * @param session
     *        The active session
     * @param data
     *        The raw data received from the client
     */
    @Override
    public void handleTCPClientChunk(AppTCPSession session, ByteBuffer data)
    {
        // use the attached SSLEngine to processes the session data
        CaptivePortalSSLEngine engine = (CaptivePortalSSLEngine) session.globalAttachment(AppSession.KEY_CAPTIVE_PORTAL_SSL_ENGINE);
        engine.handleClientData(session, data);
    }
}
