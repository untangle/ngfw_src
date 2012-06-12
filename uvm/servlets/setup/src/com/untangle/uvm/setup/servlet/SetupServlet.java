/**
 * $Id: SetupServlet.java,v 1.00 2012/06/05 18:55:40 dmorris Exp $
 */
package com.untangle.uvm.setup.servlet;

import java.io.IOException;

import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;

/**
 * A servlet which will display the start page
 */
@SuppressWarnings("serial")
public class SetupServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        UvmContext context = UvmContextFactory.context();
        request.setAttribute( "skinSettings", context.skinManager().getSettings());
        request.setAttribute( "timezone", context.adminManager().getTimeZone());

        Map<String,String> languageMap = context.languageManager().getTranslations( "untangle-libuvm" );
        request.setAttribute( "languageMap", new JSONObject( languageMap ).toString());

        String url="/WEB-INF/jsp/setup.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        rd.forward(request, response);
    }
}
