/**
 * $Id$
 */
package com.untangle.uvm.setup.jabsorb;

import java.io.IOException;

import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.TransactionRolledbackException;

import com.untangle.uvm.servlet.ServletUtils;

import org.apache.log4j.Logger;
import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCServlet;

/**
 * Initializes the JSONRPCBridge.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class UtJsonRpcServlet extends JSONRPCServlet
{
    private static final String BRIDGE_ATTRIBUTE = "SetupJSONRPCBridge";

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

        SetupContext sc = SetupContextImpl.makeSetupContext();
        bridge.registerObject("SetupContext", sc, SetupContext.class);
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

    public interface SetupContext
    {
        public void setLanguage( String language );
        
        public void setAdminPassword( String password ) throws TransactionRolledbackException;
        
        public void setTimeZone( TimeZone timeZone ) throws TransactionRolledbackException;

        public String getOemName( );
    }
}
