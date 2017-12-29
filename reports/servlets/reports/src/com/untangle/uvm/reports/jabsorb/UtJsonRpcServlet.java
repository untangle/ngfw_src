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
import com.untangle.app.reports.ReportsManager;

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

    /**
     * Initialize servlet/
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

        ReportsContext rc = ReportsContextImpl.makeReportsContext();
        bridge.registerObject("ReportsContext", rc, ReportsContext.class);
    }

    /**
     * Handle service calls.
     *
     * @param req
     *  HTTP request
     * @param resp
     *  HTTP response.
     * @throws IOException
     *  If I/O exception encountered.
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

    /**
     * Find the JSONRPCBridge from the current session.
     * If it can't be found in the session, or there is no session,
     * then return the global bridge.
     *
     * @param request The message received
     * @return the JSONRPCBridge to use for this request
     */
    protected JSONRPCBridge findBridge(HttpServletRequest request)
    {
        // Find the JSONRPCBridge for this session or create one
        // if it doesn't exist
        HttpSession session = request.getSession( false );
        JSONRPCBridge jsonBridge = null;
        if (session != null) jsonBridge = (JSONRPCBridge) session.getAttribute( BRIDGE_ATTRIBUTE );

        if ( jsonBridge == null) {
            /* Use the global bridge if it can't find the session bridge. */
            jsonBridge = JSONRPCBridge.getGlobalBridge();
            if ( logger.isDebugEnabled()) logger.debug("Using global bridge.");
        }
        return jsonBridge;
    }

    /**
     * Reports Context.
     */
    public interface ReportsContext
    {
        /**
         * Get report manager.
         *
         * @return
         *  Report manager.
         */
        public ReportsManager reportsManager();

        /**
         * Get skin manager.
         *
         * @return
         *  Skin manager.
         */
        public SkinManager skinManager();

        /**
         * Get language manager.
         *
         * @return
         *  language manager.
         */
        public LanguageManager languageManager();

        /**
         * Get current time in milliseconds.
         *
         * @return
         *  Current time in miliseconds.
         */
        public long getMilliseconds();

        /**
         * Get system timezone offset.
         *
         * @return
         *  Timeozne offset in seconds.
         */
        public Integer getTimeZoneOffset();
    }
}
