/**
 * $Id: StartPageServlet.java 37267 2014-02-26 23:42:19Z dmorris $
 */
package com.untangle.uvm.webui.servlet;

import java.io.IOException;
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.UvmContextFactory;

/**
 * A servlet which will display the start page
 *
 */
@SuppressWarnings("serial")
public class StartPageOldServlet extends HttpServlet
{
    /* ??? Perhaps this should live in a global place. */
    private static final int STORE_WINDOW_ID = new Random().nextInt(Integer.MAX_VALUE);

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String url="/WEB-INF/jsp/startPageOld.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        String companyName = UvmContextFactory.context().brandingManager().getCompanyName();
        req.setAttribute( "companyName", companyName );
        req.setAttribute( "storeWindowId", STORE_WINDOW_ID );
        req.setAttribute( "buildStamp", getServletConfig().getInitParameter("buildStamp") );
        rd.forward(req, resp);
    }

}
