/*
 * $Id: UnblockerServlet.java,v 1.00 2011/09/15 09:50:45 dmorris Exp $
 */
package com.untangle.node.webfilter;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeSettings;

@SuppressWarnings("serial")
public class UnblockerServlet extends HttpServlet
{
    // HttpServlet methods ----------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        resp.setContentType("text/xml");
        resp.addHeader("Cache-Control", "no-cache");

        String nonce = req.getParameter("nonce");
        String tidStr = req.getParameter("tid");
        boolean global = Boolean.parseBoolean(req.getParameter("global"));

        try {
            NodeManager tman = UvmContextFactory.context().nodeManager();
            WebFilter tran = (WebFilter) tman.node( Long.parseLong(tidStr) );

            if (tran.unblockSite(nonce, global)) {
                resp.getOutputStream().println("<success/>");
            } else {
                resp.getOutputStream().println("<failure/>");
            }
        } catch (IOException exn) {
            throw new ServletException(exn);
        }
    }
}

