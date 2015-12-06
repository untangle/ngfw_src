/**
 * $Id$
 */
package com.untangle.node.web_filter;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeSettings;

@SuppressWarnings("serial")
public class WebFilterUnblockerServlet extends HttpServlet
{

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        resp.setContentType("text/xml");
        resp.addHeader("Cache-Control", "no-cache");

        String nonce = req.getParameter("nonce");
        String tidStr = req.getParameter("tid");
        String password = req.getParameter("password");
        boolean global = Boolean.parseBoolean(req.getParameter("global"));

        try {
            NodeManager tman = UvmContextFactory.context().nodeManager();
            Long nodeId = Long.parseLong(tidStr);
            WebFilterApp node = (WebFilterApp) tman.node( nodeId );

            if (node.unblockSite(nonce, global, password)) {
                resp.getOutputStream().println("<success/>");
            } else {
                resp.getOutputStream().println("<failure/>");
            }
        } catch (IOException exn) {
            throw new ServletException(exn);
        }
    }
}

