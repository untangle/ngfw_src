/**
 * $Id: Downloader.java,v 1.00 2013/04/24 15:55:59 dmorris Exp $
 */
package com.untangle.node.openvpn.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.util.ServletStreamer;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class Downloader extends HttpServlet
{
    private static final String CONFIG_PAGE        = "/config.zip";
    private static final String CONFIG_NAME_PREFIX = "config-";
    private static final String CONFIG_NAME_SUFFIX = ".zip";
    private static final String CONFIG_TYPE        = "application/zip";

    private static final String SETUP_PAGE        = "/setup.exe";
    private static final String SETUP_NAME_PREFIX = "setup-";
    private static final String SETUP_NAME_SUFFIX = ".exe";
    private static final String SETUP_TYPE        = "application/download";

    private final Logger logger = Logger.getLogger( this.getClass());

    protected void service( HttpServletRequest request,  HttpServletResponse response ) throws ServletException, IOException
    {
        /* XXX FIXME require ADMIN */

        String commonName = request.getParameter( "client" );
        String fileName = null;
        String downloadFilename = null;
        String pageName = request.getServletPath();
        String type = "";

        if ( pageName.equalsIgnoreCase( CONFIG_PAGE )) {
            fileName = "/tmp/openvpn/client-packages/" + "config-" + commonName + ".zip";
            downloadFilename = "openvpn-" + commonName + "-config.zip";
            type     = CONFIG_TYPE;
        } else if ( pageName.equalsIgnoreCase( SETUP_PAGE )) {
            fileName = "/tmp/openvpn/client-packages/" + "setup-" + commonName + ".exe";
            downloadFilename = "openvpn-" + commonName + "-setup.exe";
            type     = SETUP_TYPE;
        } else {
            fileName = null;
            downloadFilename = null;
        }

        /* File name shouldn't be null unless the web.xml is misconfigured to force pages
         * that are not supposed to reach here */
        if ( ( commonName == null ) || ( fileName == null ) || ( downloadFilename == null ) ) {
            request.setAttribute( "com.untangle.node.openvpn.servlet.reason", "downloadFilename or fileName is null [" + pageName + "]" );
            rejectFile( request, response );
        } else {
            streamFile( request, response, fileName, downloadFilename, type );
        }
    }

    /** Send a file to a user */
    /**
     * @param fileName - Full path of the file to download
     * @param downloadFileName - Name that should be given to the file that is downloaded
     */
    void streamFile( HttpServletRequest request, HttpServletResponse response, String fileName, String downloadFileName, String type )
        throws ServletException, IOException
    {
        InputStream fileData;

        long length = 0;

        logger.debug( "Streaming '" + fileName + "'" );

        try {
            File file = new File( fileName );            
            fileData  = new FileInputStream( file );
            length = file.length();
        } catch ( FileNotFoundException e ) {
            logger.info( "The file '" + fileName + "' does not exist" );
            request.setAttribute( "com.untangle.node.openvpn.servlet.reason", "The file '" + fileName + "' does not exist" );
            rejectFile( request, response );
            return;
        }

        ServletStreamer ss = ServletStreamer.getInstance();

        ss.stream( request, response, fileData, downloadFileName, type, length );
    }

    void rejectFile( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        request.setAttribute( "com.untangle.node.openvpn.servlet.debugging", "" );
        request.setAttribute( "com.untangle.node.openvpn.servlet.valid", false );

        /* Indicate that the response was not rejected */
        response.setStatus( HttpServletResponse.SC_FORBIDDEN );
        request.getRequestDispatcher( "/Index.jsp" ).forward( request, response );
    }
}
