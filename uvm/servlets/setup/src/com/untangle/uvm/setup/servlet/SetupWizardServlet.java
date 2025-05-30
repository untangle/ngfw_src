/**
 * $Id$
 */
package com.untangle.uvm.setup.servlet;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A servlet which will display the start page of new setup wizard
 */
@SuppressWarnings("serial")
public class SetupWizardServlet extends HttpServlet
{

    /**
     * doGet - handle GET requests
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        UvmContext context = UvmContextFactory.context();
        request.setAttribute( "buildStamp", getServletConfig().getInitParameter("buildStamp") );
        request.setAttribute( "skinName", context.skinManager().getSettings().getSkinName());
        request.setAttribute( "extjsTheme", context.skinManager().getSkinInfo().getExtjsTheme());

        String url="/WEB-INF/jsp/setup-wizard.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        rd.forward(request, response);
    }
}
