/**
 * $Id: StartPageServlet.java,v 1.00 2012/06/12 12:33:13 dmorris Exp $
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
 * @author Catalin Matei <cmatei@untangle.com>
 */
@SuppressWarnings("serial")
public class StartPageServlet extends HttpServlet
{
    /* ??? Perhaps this should live in a global place. */
    private static final int STORE_WINDOW_ID = new Random().nextInt(Integer.MAX_VALUE);

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String url="/WEB-INF/jsp/startPage.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        String companyName = UvmContextFactory.context().brandingManager().getCompanyName();
        boolean isWizardComplete = UvmContextFactory.context().isWizardComplete();
        req.setAttribute( "isWizardComplete", isWizardComplete ? "true" : "false" );
        req.setAttribute( "companyName", companyName );
        req.setAttribute( "storeWindowId", STORE_WINDOW_ID );
        req.setAttribute( "buildStamp", getServletConfig().getInitParameter("buildStamp") );
        rd.forward(req, resp);
    }

}
