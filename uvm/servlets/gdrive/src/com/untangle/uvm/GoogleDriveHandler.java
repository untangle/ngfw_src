/**
 * $Id: GoogleDriveHandler.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.uvm;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

/**
 * GoogleDrive http servelet class
 */
@SuppressWarnings("serial")
public class GoogleDriveHandler extends HttpServlet
{
    private final Logger logger = Logger.getLogger( this.getClass() );

    /**
     * Perform HTTP GET operation
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet( HttpServletRequest request,  HttpServletResponse response ) throws ServletException, IOException
    {
        String code = request.getParameter( "code" );

        if ( code == null || "".equals(code) ) {
            logger.warn("Missing code argument.");
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        logger.info("Google Drive registration: " + code);
        
        UvmContext uvmContext = UvmContextFactory.context();
        String error = uvmContext.googleManager().provideDriveCode( code );

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();        
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Google Drive Configuration</title>");

        if ( error == null ) {
            writer.println("<script type=\"text/javascript\">");
            writer.println("window.onload = function(){ close() };");
            writer.println("</script>");
        }
        
        writer.println("</head>");
        writer.println("<body bgcolor=white>");

        if ( error == null ) {
            writer.println("Google Drive Configuration successful!");
        } else {
            writer.println("Google Drive Configuration Failed: " + error);
        }

        writer.println("</body>");
        writer.println("</html>");

        return;
    }
}
