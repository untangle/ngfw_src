/**
 * $Id$
 */
package com.untangle.uvm.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.servlet.DownloadHandler;

/**
 * A servlet for import / export grid settings 
 */
@SuppressWarnings({ "serial", "unchecked" })
public class DownloadServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String CHARACTER_ENCODING = "utf-8";
    
    /**
     * doPost handle a POST
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        processExport(req, resp);
    }

    /**
     * processExport - process an export
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    private void processExport(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        String downloadType = req.getParameter("type");

        DownloadHandler handler = UvmContextFactory.context().servletFileManager().getDownloadHandler( downloadType );
        
        if ( handler == null ) {
            logger.error("Unable to handle an download of type: " + downloadType );
            return;
        }

        logger.info("Serving Download: " + downloadType);
        handler.serveDownload( req, resp );
    }
    
}
