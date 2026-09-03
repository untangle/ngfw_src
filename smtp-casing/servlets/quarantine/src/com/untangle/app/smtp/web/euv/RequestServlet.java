/**
 * $Id: RequestServlet.java 36445 2013-11-20 00:04:22Z dmorris $
 */
package com.untangle.app.smtp.web.euv;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet used when requesting a digest email/login.
 */
@SuppressWarnings("serial")
public class RequestServlet extends HttpServlet
{
    private static final String REQ_DIGEST_VIEW = "/console/quarantine";

    /**
     * Setup the request servlet
     *
     * @param  req              HttpServletRequest object.
     * @param  resp             HttpServletResponse object.
     * @throws ServletException If there's a problem with the servlet.
     * @throws IOException      General input/output error.
     */
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        resp.sendRedirect(REQ_DIGEST_VIEW);
    }
}
