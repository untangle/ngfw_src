/**
 * $Id$
 */
package com.untangle.node.phish;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NodeSettings;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeContext;

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
            NodeContext tctx = tman.nodeContext( Long.parseLong(tidStr) );
            Phish tran = (Phish)tctx.node();

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

