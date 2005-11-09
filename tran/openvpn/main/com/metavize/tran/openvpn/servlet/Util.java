/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.openvpn.servlet;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import static com.metavize.tran.openvpn.Constants.VPN_CONF_BASE;

class Util
{
    private static final Util INSTANCE = new Util();

    private static final String BASE_DIRECTORY = VPN_CONF_BASE + "/openvpn/client-packages";

    private final Logger logger = Logger.getLogger( this.getClass());

    private Util()
    {
    }

    /* Returns the commonName for the request, or null if the request is not valid */
    String getCommonName( HttpServletRequest request )
    {
        /* XXXXXXXXXXXXXXXXX */
        return "rbscott";
    }
    
    /** Send a file to a user */
    /**
     * @param fileName - Full path of the file to download
     * @param downloadFileName - Name that should be given to the file that is downloaded
     */
    void downloadFile( HttpServletRequest request, HttpServletResponse response, 
                          String fileName, String downloadFileName )
    {
        response.setContentType( "application/download" );
        response.setHeader( "Content-Disposition", "attachment;filename=\"" + downloadFileName + "\"" );

        fileName = BASE_DIRECTORY + fileName;
        
        InputStream fileData;

        try {
            fileData  = new FileInputStream( new File( fileName ));
        } catch ( FileNotFoundException e ) { 
            logger.info( "The file '" + fileName + "' does not exist" );
            rejectFile( request, response );
            return;
        }

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

    void rejectFile( HttpServletRequest request, HttpServletResponse response )
    {
        
    }

    static Util getInstance()
    {
        return INSTANCE;
    }
}
