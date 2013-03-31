/**
 * $Id$
 */
package com.untangle.uvm.webui.jabsorb;

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

    // HttpServlet methods ----------------------------------------------------

    @SuppressWarnings("unchecked") //getAttribute
    public void init()
    {
        threadRequest = (InheritableThreadLocal<HttpServletRequest>)getServletContext().getAttribute("threadRequest");
        if (null == threadRequest) {
            logger.warn("could not get threadRequest");
        }
    }

    public void service(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        if (null != threadRequest) {
            threadRequest.set(req);
        }

        initSessionBridge(req);

        super.service(req, resp);

        if (null != threadRequest) {
            threadRequest.set(null);
        }
    }

    // private methods --------------------------------------------------------

    private void initSessionBridge(HttpServletRequest req)
    {
        HttpSession s = req.getSession();
        JSONRPCBridge b = (JSONRPCBridge)s.getAttribute(BRIDGE_ATTRIBUTE);

        if (null == b) {
            b = new JSONRPCBridge();
            s.setAttribute(BRIDGE_ATTRIBUTE, b);

            try {
                ServletUtils.getInstance().registerSerializers(b);
            } catch (Exception e) {
                logger.warn( "Unable to register serializers", e );
            }

            b.setCallbackController(new UtCallbackController(b));

            UvmContext uvm = UvmContextFactory.context();
            b.registerObject("UvmContext", uvm, UvmContext.class);

            /**
             * This section registers hints that these classes should always be
             * serialized as callable references
             */
            try {
                b.registerCallableReference(uvm.languageManager().getClass());
                b.registerCallableReference(uvm.localDirectory().getClass());
                b.registerCallableReference(uvm.brandingManager().getClass());
                b.registerCallableReference(uvm.skinManager().getClass());
                b.registerCallableReference(uvm.messageManager().getClass());
                b.registerCallableReference(uvm.languageManager().getClass());
                b.registerCallableReference(uvm.certificateManager().getClass());
                b.registerCallableReference(uvm.aptManager().getClass());
                b.registerCallableReference(uvm.nodeManager().getClass());
                b.registerCallableReference(uvm.loggingManager().getClass());
                b.registerCallableReference(uvm.mailSender().getClass());
                b.registerCallableReference(uvm.adminManager().getClass());
                b.registerCallableReference(uvm.systemManager().getClass());
                b.registerCallableReference(uvm.networkManager().getClass());
                b.registerCallableReference(uvm.getConnectivityTester().getClass());
                b.registerCallableReference(uvm.argonManager().getClass());
                b.registerCallableReference(uvm.licenseManager().getClass());
                b.registerCallableReference(uvm.uploadManager().getClass());
                b.registerCallableReference(uvm.settingsManager().getClass());
                b.registerCallableReference(uvm.oemManager().getClass());
                b.registerCallableReference(uvm.alertManager().getClass());
                b.registerCallableReference(uvm.pipelineFoundry().getClass());
                b.registerCallableReference(uvm.sessionMonitor().getClass());
                b.registerCallableReference(uvm.execManager().getClass());
                b.registerCallableReference(uvm.tomcatManager().getClass());
                b.registerCallableReference(uvm.hostTable().getClass());

                b.registerCallableReference(com.untangle.uvm.node.Node.class);
            }
            catch (Exception e) {
                logger.warn("Exception registering callable reference classes",e);
            }
        }
    }
}
