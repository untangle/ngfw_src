/**
 * $Id$
 */
package com.untangle.uvm.admin.jabsorb;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCServlet;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.servlet.ServletUtils;

/**
 * Initializes the JSONRPCBridge.
 */
@SuppressWarnings("serial")
public class UtJsonRpcServlet extends JSONRPCServlet
{
    private static final String BRIDGE_ATTRIBUTE = "JSONRPCBridge";

    private final Logger logger = Logger.getLogger(getClass());

    private InheritableThreadLocal<HttpServletRequest> threadRequest;

    private JSONRPCBridge bridge;
    private UtCallbackController callback;
    
    /**
     * init
     */
    @SuppressWarnings("unchecked") //getAttribute
    public void init()
    {
        threadRequest = (InheritableThreadLocal<HttpServletRequest>)getServletContext().getAttribute("threadRequest");
        if (null == threadRequest) {
            logger.warn("could not get threadRequest");
        }

        bridge = new JSONRPCBridge();
        callback = new UtCallbackController( bridge );
        bridge.setCallbackController( callback );
        
        try {
            ServletUtils.getInstance().registerSerializers(bridge);
        } catch (Exception e) {
            logger.warn( "Unable to register serializers", e );
        }

        UvmContext uvm = UvmContextFactory.context();
        bridge.registerObject("UvmContext", uvm, UvmContext.class);

        /**
         * This section registers hints that these classes should always be
         * serialized as callable references
         */
        try {
            bridge.registerCallableReference(uvm.languageManager().getClass());
            bridge.registerCallableReference(uvm.localDirectory().getClass());
            bridge.registerCallableReference(uvm.brandingManager().getClass());
            bridge.registerCallableReference(uvm.skinManager().getClass());
            bridge.registerCallableReference(uvm.metricManager().getClass());
            bridge.registerCallableReference(uvm.languageManager().getClass());
            bridge.registerCallableReference(uvm.certificateManager().getClass());
            bridge.registerCallableReference(uvm.appManager().getClass());
            bridge.registerCallableReference(uvm.loggingManager().getClass());
            bridge.registerCallableReference(uvm.mailSender().getClass());
            bridge.registerCallableReference(uvm.adminManager().getClass());
            bridge.registerCallableReference(uvm.eventManager().getClass());
            bridge.registerCallableReference(uvm.uriManager().getClass());
            bridge.registerCallableReference(uvm.authenticationManager().getClass());
            bridge.registerCallableReference(uvm.systemManager().getClass());
            bridge.registerCallableReference(uvm.networkManager().getClass());
            bridge.registerCallableReference(uvm.getConnectivityTester().getClass());
            bridge.registerCallableReference(uvm.netcapManager().getClass());
            bridge.registerCallableReference(uvm.licenseManager().getClass());
            bridge.registerCallableReference(uvm.servletFileManager().getClass());
            bridge.registerCallableReference(uvm.settingsManager().getClass());
            bridge.registerCallableReference(uvm.oemManager().getClass());
            bridge.registerCallableReference(uvm.notificationManager().getClass());
            bridge.registerCallableReference(uvm.pipelineFoundry().getClass());
            bridge.registerCallableReference(uvm.sessionMonitor().getClass());
            bridge.registerCallableReference(uvm.execManager().getClass());
            bridge.registerCallableReference(uvm.tomcatManager().getClass());
            bridge.registerCallableReference(uvm.dashboardManager().getClass());
            bridge.registerCallableReference(uvm.hostTable().getClass());
            bridge.registerCallableReference(uvm.deviceTable().getClass());
            bridge.registerCallableReference(uvm.userTable().getClass());

            bridge.registerCallableReference(com.untangle.uvm.app.App.class);
        }
        catch (Exception e) {
            logger.warn("Exception registering callable reference classes",e);
        }
    }

    /**
     * service
     * @param req
     * @param resp
     * @throws IOException
     */
    public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        if (null != threadRequest) {
            threadRequest.set(req);
        }

        HttpSession s = req.getSession();
        JSONRPCBridge b = (JSONRPCBridge)s.getAttribute(BRIDGE_ATTRIBUTE);
        if ( b == null ) {
            s.setAttribute(BRIDGE_ATTRIBUTE, bridge);
        }

        super.service(req, resp);

        if (null != threadRequest) {
            threadRequest.set(null);
        }
    }
}
