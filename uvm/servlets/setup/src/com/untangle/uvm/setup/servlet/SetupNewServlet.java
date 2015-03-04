/**
 * $Id: SetupServlet.java 39296 2014-12-16 12:40:54Z vdumitrescu $
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
public class SetupNewServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        UvmContext context = UvmContextFactory.context();
        request.setAttribute( "buildStamp", getServletConfig().getInitParameter("buildStamp") );
        request.setAttribute( "skinSettings", context.skinManager().getSettings());

        String url="/WEB-INF/jsp/setupNew.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        rd.forward(request, response);
    }
}
