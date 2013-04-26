/**
 * $Id$
 */
package com.untangle.uvm.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/* Used to stream data to as the response to a servlet request */
public class ServletStreamer
{
    private static final ServletStreamer INSTANCE = new ServletStreamer();

    private final Logger logger = Logger.getLogger( this.getClass());

    private ServletStreamer()
    {
    }

    public void stream( HttpServletRequest request, HttpServletResponse response, String fileName, String downloadFileName, String type )
        throws ServletException, IOException
    {
        stream( request, response, new File( fileName ), downloadFileName, type );
    }

    public void stream( HttpServletRequest request, HttpServletResponse response, File file, String downloadFileName, String type )
        throws ServletException, IOException
    {
        try {
            stream( request, response, new FileInputStream( file ), downloadFileName, type, file.length());
        } catch ( FileNotFoundException e ) {
            logger.info( "The file '" + file + "' does not exist" );
            /* Indicate that the response was not rejected */
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }
    }
    
    /**
     * Stream a file instead of sending to a client.
     * @param request the request from the client.
     * @param response The Response to the client.
     * @param inputStream the stream to send.
     * @param downloadFileName if non-null, send this file as an attachment rather than an inline file.
     * @param type Mime type of the file.
     * @param length Length of the file, if greater than zero this is set in the header.
     */
    public void stream( HttpServletRequest request, HttpServletResponse response, InputStream inputStream, String downloadFileName, String type, long length )
        throws ServletException, IOException
    {
        response.setContentType( type );

        if ( downloadFileName != null ) {
            response.setHeader( "Content-Disposition", "attachment; filename=\"" + downloadFileName + "\"" );
        }
        if ( length > 0 ) {
            response.setHeader( "Content-Length", "" + length );
        }

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        OutputStream out = null;

        try {
            out = response.getOutputStream();
            bis = new BufferedInputStream( inputStream );
            bos = new BufferedOutputStream( out );
            byte[] buff = new byte[2048];
            int bytesRead;
            while( -1 != ( bytesRead = bis.read( buff, 0, buff.length ))) bos.write( buff, 0, bytesRead );
        } catch ( Exception e ) {
            logger.warn( "Error streaming file.", e );
        } finally {
            try {
                if ( bis != null ) bis.close();
            } catch ( Exception e ) {
                logger.warn( "Error closing input stream", e );
            }

            try {
                if ( bos != null ) bos.close();
            } catch ( Exception e ) {
                logger.warn( "Error closing output stream", e );
            }

            try {
                if ( out != null ) out.close();
            } catch ( Exception e ) {
                logger.warn( "Error closing output stream", e );
            }
        }
    }

    public static ServletStreamer getInstance()
    {
        return INSTANCE;
    }
}
