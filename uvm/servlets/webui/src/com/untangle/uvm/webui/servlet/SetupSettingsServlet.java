/**
 * $Id$
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
import com.untangle.uvm.networking.NetworkConfiguration;
import com.untangle.uvm.networking.InterfaceConfiguration;
import com.untangle.uvm.networking.NetworkUtil;
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

        NetworkManager nm = context.networkManager();

        NetworkConfiguration networkConfiguration = nm.getNetworkConfiguration();
        InterfaceConfiguration wanConfig = networkConfiguration.findFirstWAN();

        try {
            request.setAttribute( "interfaceArray", js.toJSON( networkConfiguration.getInterfaceList()));
            request.setAttribute( "wanConfiguration", js.toJSON( wanConfig ));
        } catch ( MarshallException e ) {
            throw new ServletException( "Unable to serializer JSON", e );
        }

        String url="/WEB-INF/jsp/setupSettings.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        response.setContentType("text/javascript");
        rd.forward(request, response);
    }
}
