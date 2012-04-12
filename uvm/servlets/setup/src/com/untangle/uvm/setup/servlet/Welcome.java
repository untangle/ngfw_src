/**
 * $Id: Welcome.java,v 1.00 2012/04/11 14:44:40 dmorris Exp $
 */
package com.untangle.uvm.setup.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.UvmContextFactory;

/**
 * A servlet which will display the start page
 */
@SuppressWarnings("serial")
public class Welcome extends HttpServlet
{
    private static final String WEBUI_URL = "/webui/startPageNew.do";
    private static final String SETUP_URL = "/setup/languageNew.do";
        
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {        
        String url = WEBUI_URL;

        /* If the server is not activated send them to the setup page. */
        if ( !UvmContextFactory.context().isWizardComplete() ) url = SETUP_URL;

        if (request.getParameter("console") != null && request.getParameter("console").equals("1")) {
            url = url + "?console=1";
        }

        response.sendRedirect( url );
    }
}
