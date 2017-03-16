/**
 * $Id$
 */
package com.untangle.app.smtp.web.euv;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCServlet;

/**
 * Initializes the JSONRPCBridge.
 */
@SuppressWarnings("serial")
public class UtJsonRpcServlet extends JSONRPCServlet
{
    private static final String BRIDGE_ATTRIBUTE = "JSONRPCBridge";

    public void service(HttpServletRequest req, HttpServletResponse resp)
        throws IOException
    {
        initSessionBridge(req);
        super.service(req, resp);
    }

    private void initSessionBridge(HttpServletRequest req)
    {
        HttpSession s = req.getSession();
        JSONRPCBridge b = (JSONRPCBridge)s.getAttribute(BRIDGE_ATTRIBUTE);
        if (null == b) {
            b = new JSONRPCBridge();
            s.setAttribute(BRIDGE_ATTRIBUTE, b);
            
            b.registerObject("Quarantine", JsonInterfaceImpl.getInstance(), JsonInterface.class);
        }
    }
}
