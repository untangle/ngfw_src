/*
 * $Id: OauthHandler.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.node.directory_connector;

import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;
import com.untangle.node.directory_connector.DirectoryConnectorApp;
import com.untangle.node.directory_connector.LoginEvent;

@SuppressWarnings("serial")
public class OauthHandler extends HttpServlet
{
    private final Logger logger = Logger.getLogger( this.getClass() );

    protected void doGet( HttpServletRequest request,  HttpServletResponse response ) throws ServletException, IOException
    {
        DirectoryConnectorApp directoryConnector = (DirectoryConnectorApp)UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");

        String code = request.getParameter( "code" );

        if ( code == null || "".equals(code) ) {
            logger.warn("Missing code argument.");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        logger.info("OAUTH registration: " + code);
        
        String error = directoryConnector.getGoogleManager().provideDriveCode( code );

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();        
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Oauth Configuration</title>");

        if ( error == null ) {
            writer.println("<script type=\"text/javascript\">");
            writer.println("window.onload = function(){ close() };");
            writer.println("</script>");
        }
        
        writer.println("</head>");
        writer.println("<body bgcolor=white>");

        if ( error == null ) {
            writer.println("Oauth Configuration successful!");
        } else {
            writer.println("Oauth Configuration Failed: " + error);
        }

        writer.println("</body>");
        writer.println("</html>");

        return;
    }
}
    
