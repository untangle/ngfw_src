/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.user;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.tran.script.ScriptRunner;
import org.apache.log4j.Logger;

public class Downloader extends HttpServlet
{
    private static final String WMI_INSTALLER_FILE_NAME = "/tmp/setup-wmi.exe";
    private static final String WMI_DOWNLOAD_NAME = "setup.exe";
    private static final String WMI_DOWNLOAD_TYPE = "application/download";
    private static final String GENERATE_INSTALLER_SCRIPT =
        System.getProperty( "bunnicula.home" ) + "/installers/wmimapper/makeinstaller";

    private final Logger logger = Logger.getLogger( this.getClass());

    protected void doGet( HttpServletRequest request,  HttpServletResponse response )
        throws ServletException, IOException
    {
        generateInstaller();
        streamFile( request, response, WMI_INSTALLER_FILE_NAME, WMI_DOWNLOAD_NAME, WMI_DOWNLOAD_TYPE );
    }

    private void generateInstaller() throws ServletException
    {
        try {
            ScriptRunner.getInstance().exec( GENERATE_INSTALLER_SCRIPT );
        } catch ( TransformException e ) {
            logger.warn( "error running script", e );
            throw new ServletException( "Unable to create WMI Installer, please try again later." );
        }
    }

    /* Xxxxxxxxxxxxxxxxxx copied from openvpn, should be in a global util class in api */

    /** Send a file to a user */
    /**
     * @param fileName - Full path of the file to download
     * @param downloadFileName - Name that should be given to the file that is downloaded
     */
    private void streamFile( HttpServletRequest request, HttpServletResponse response,
                     String fileName, String downloadFileName, String type )
        throws ServletException, IOException
    {
        InputStream fileData;

        long length = 0;

        try {
            File file = new File( fileName );
            fileData  = new FileInputStream( file );
            length = file.length();
        } catch ( FileNotFoundException e ) {
            logger.info( "The file '" + fileName + "' does not exist" );

            /* Indicate that the response was not rejected */
            response.sendError( HttpServletResponse.SC_FORBIDDEN );
            return;
        }

        response.setContentType( type );
        response.setHeader( "Content-Disposition", "attachment;filename=\"" + downloadFileName + "\"" );
        response.setHeader( "Content-Length", "" + length );

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        OutputStream out = null;

        try {
            out = response.getOutputStream();
            bis = new BufferedInputStream( fileData );
            bos = new BufferedOutputStream(out);
            byte[] buff = new byte[2048];
            int bytesRead;
            while( -1 != ( bytesRead = bis.read( buff, 0, buff.length ))) bos.write( buff, 0, bytesRead );
        } catch ( Exception e ) {
            logger.warn( "Error streaming file.", e );
        } finally {
            try {
                if ( bis != null ) bis.close();
            } catch ( Exception e ) {
                logger.warn( "Error closing input stream [" + fileName + "]", e );
            }

            try {
                if ( bos != null ) bos.close();
            } catch ( Exception e ) {
                logger.warn( "Error closing output stream [" + fileName + "]", e );
            }

            try {
                if ( out != null ) out.close();
            } catch ( Exception e ) {
                logger.warn( "Error closing output stream [" + fileName + "]", e );
            }
        }
    }
}
