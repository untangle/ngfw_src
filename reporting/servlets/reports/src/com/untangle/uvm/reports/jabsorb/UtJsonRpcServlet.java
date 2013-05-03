/**
 * $Id$
 */
package com.untangle.uvm.reports.jabsorb;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCServlet;

import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.SkinManager;
import com.untangle.node.reporting.ReportingManager;

import com.untangle.uvm.servlet.ServletUtils;

/**
 * Initializes the JSONRPCBridge.
 */
@SuppressWarnings("serial")
public class UtJsonRpcServlet extends JSONRPCServlet
{
    private static final String BRIDGE_ATTRIBUTE = "ReportsJSONRPCBridge";

    private final Logger logger = Logger.getLogger(getClass());

    private InheritableThreadLocal<HttpServletRequest> threadRequest;

    private JSONRPCBridge bridge;
    private UtCallbackController callback;
    
    // HttpServlet methods ----------------------------------------------------

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

        ReportsContext rc = ReportsContextImpl.makeReportsContext();
        bridge.registerObject("ReportsContext", rc, ReportsContext.class);
    }

    public void service(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
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

    public interface ReportsContext
    {
        public ReportingManager reportingManager();

        public SkinManager skinManager();

        public LanguageManager languageManager();                
    }
}
