/**
 * $Id: SetupSettingsServlet.java 34022 2013-02-26 19:14:43Z dmorris $
 */
package com.untangle.uvm.webui.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.MarshallException;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.servlet.ServletUtils;

/**
 * A servlet which will display the start page
 */
@SuppressWarnings("serial")
public class SetupSettingsServlet extends HttpServlet
{
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        UvmContext context = UvmContextFactory.context();

        JSONSerializer js = new JSONSerializer();
        try {
            ServletUtils.getInstance().registerSerializers(js);
        } catch ( Exception e ) {
            throw new ServletException( "Unable to load the default serializer", e );
        }

        String url="/WEB-INF/jsp/setupSettings.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        response.setContentType("text/javascript");
        rd.forward(request, response);
    }
}
