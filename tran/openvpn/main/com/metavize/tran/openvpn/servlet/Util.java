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
import java.io.IOException;
import java.io.FileInputStream;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;

import com.metavize.mvvm.client.MvvmRemoteContextFactory;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.security.Tid;

import com.metavize.tran.openvpn.VpnTransform;
import com.metavize.tran.openvpn.VpnSettings;

import org.apache.log4j.Logger;

import static com.metavize.tran.openvpn.Constants.VPN_CONF_BASE;

class Util
{
    private static final Util INSTANCE = new Util();

    private static final String BASE_DIRECTORY = VPN_CONF_BASE + "/openvpn/client-packages";

    public static final  String DISTRIBUTION_KEY_PARAM = "key";

    static final String ATTR_BASE = "com.metavize.tran.openvpn.servlet.";

    private static final String EXPIRATION_SESSION_ATTR  = ATTR_BASE + "expiration";
    private static final String COMMON_NAME_SESSION_ATTR = ATTR_BASE + "common-name";


    static final String STATUS_ATTR      = ATTR_BASE + "status";
    static final String COMMON_NAME_ATTR = ATTR_BASE + "common-name";

    /* Download in at least a half hour, seems reasonable */
    private static final long TIMEOUT = 1000 * 60 * 30;

    private final Logger logger = Logger.getLogger( this.getClass());

    private Util()
    {
    }

    /* Returns the commonName for the request, or null if the request is not valid */
    String getCommonName( HttpServletRequest request )
    {
        String key = request.getParameter( DISTRIBUTION_KEY_PARAM );
        
        if ( key != null ) key = key.trim();

        logger.debug( "key is : " + key );

        if ( key != null && key.length() > 0 ) {
            String commonName = null;
            
            try {
                /* XXX Should this be cached?? */
                MvvmRemoteContext ctx = MvvmRemoteContextFactory.factory().systemLogin( 0 );
                Tid tid = ctx.transformManager().transformInstances( "openvpn-transform" ).get( 0 );
                TransformContext tc = ctx.transformManager().transformContext( tid );
                commonName = ((VpnTransform)tc.transform()).lookupClientDistributionKey( key );
            } catch ( Exception e ) {
                logger.error( "Error connecting to the openvpn transform", e );
                return null;
            }

            if ( commonName == null ) return null;

            HttpSession session = request.getSession( true );
            
            session.setAttribute( COMMON_NAME_SESSION_ATTR, commonName );
            session.setAttribute( EXPIRATION_SESSION_ATTR, new Date( new Date().getTime() + TIMEOUT ));

            return commonName;
        } else {
            /* Look into the session to see if there is data about the user */
            HttpSession session = request.getSession( false );

            /* If they have no session information, then there is nothing to do */
            if ( session == null ) return null;
            Date expirationDate = (Date)session.getAttribute( EXPIRATION_SESSION_ATTR );
            String commonName = (String)session.getAttribute( COMMON_NAME_SESSION_ATTR );
            if ( commonName == null || expirationDate == null ) return null;

            /* If the session is expired, kill it */
            if ( expirationDate.after( new Date())) return null;

            return commonName;
        }
    }
    
    /** Send a file to a user */
    /**
     * @param fileName - Full path of the file to download
     * @param downloadFileName - Name that should be given to the file that is downloaded
     */
    void downloadFile( HttpServletRequest request, HttpServletResponse response, 
                          String fileName, String downloadFileName )
        throws ServletException, IOException
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
        throws ServletException, IOException
    {
        request.setAttribute( STATUS_ATTR, "ERROR" );
        request.getRequestDispatcher("/Index.jsp").forward( request, response );
    }

    static Util getInstance()
    {
        return INSTANCE;
    }
}
