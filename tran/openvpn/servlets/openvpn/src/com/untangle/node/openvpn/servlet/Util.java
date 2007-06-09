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

package com.untangle.tran.openvpn.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.tran.openvpn.Constants;
import com.untangle.tran.openvpn.VpnTransform;
import org.apache.log4j.Logger;

class Util
{
    private static final Util INSTANCE = new Util();

    private static final String BASE_DIRECTORY = Constants.PACKAGES_DIR;

    public static final  String DISTRIBUTION_KEY_PARAM = "key";

    static final String ATTR_BASE = "com.untangle.tran.openvpn.servlet.";

    private static final String EXPIRATION_SESSION_ATTR  = ATTR_BASE + "expiration";
    private static final String COMMON_NAME_SESSION_ATTR = ATTR_BASE + "common-name";

    static final String COMMON_NAME_ATTR = ATTR_BASE + "common-name";
    static final String REASON_ATTR      = ATTR_BASE + "reason";
    static final String DEBUGGING_ATTR   = ATTR_BASE + "debugging";
    static final String VALID_ATTR       = ATTR_BASE + "valid";

    /* Download in at least a half hour, seems reasonable */
    private static final long TIMEOUT = 1000 * 60 * 30;

    private final Logger logger = Logger.getLogger( this.getClass());

    private Util()
    {
    }

    /* Returns true if this page requires a secure redirect */
    boolean requiresSecure( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        if ( request.getScheme().equals( "https" )) return false;

        /* Otherwise, reject the page, they definitely didn't use the link to get it */
        rejectFile( request, response );
        return true;
    }

    /* Returns the commonName for the request, or null if the request is not valid */
    String getCommonName(HttpServlet servlet, HttpServletRequest request )
    {
        String key = request.getParameter( DISTRIBUTION_KEY_PARAM );

        if ( key != null ) key = key.trim();

        if (logger.isDebugEnabled()) {
            logger.debug( "key is : " + key );
        }

        if ( key != null && key.length() > 0 ) {
            String commonName = null;

            try {
                /* XXX Should this be cached?? */
                IPaddr address = IPaddr.parse( request.getRemoteAddr());

                MvvmLocalContext ctx = MvvmContextFactory.context();
                Tid tid = ctx.transformManager().transformInstances( "openvpn-transform" ).get( 0 );
                TransformContext tc = ctx.transformManager().transformContext( tid );
                commonName = ((VpnTransform)tc.transform()).lookupClientDistributionKey( key, address );
            } catch ( Exception e ) {
                logger.error( "Error connecting to the openvpn transform", e );
                request.setAttribute( REASON_ATTR, "Error connnecting to the openvpn transform " + e );
                return null;
            }

            if ( null == commonName ) {
                request.setAttribute( REASON_ATTR, "Key doesn't exist" );
                return null;
            }

            HttpSession session = request.getSession( true );

            session.setAttribute( COMMON_NAME_SESSION_ATTR, commonName );
            session.setAttribute( EXPIRATION_SESSION_ATTR, new Date( System.currentTimeMillis() + TIMEOUT ));

            return commonName;
        } else {
            /* Look into the session to see if there is data about the user */
            HttpSession session = request.getSession( false );

            /* If they have no session information, then there is nothing to do */
            if ( session == null ) {
                request.setAttribute( REASON_ATTR, "User doesn't have a session" );
                return null;
            }

            Date expirationDate = (Date)session.getAttribute( EXPIRATION_SESSION_ATTR );
            String commonName = (String)session.getAttribute( COMMON_NAME_SESSION_ATTR );
            if ( commonName == null || expirationDate == null ) {
                request.setAttribute( REASON_ATTR, "Common name or expiration is null" );
                return null;
            }

            /* If the session is expired, kill it */
            Date now = new Date();
            if ( now.after( expirationDate )) {
                request.setAttribute( REASON_ATTR, "Session expired at " + expirationDate );
                return null;
            }

            return commonName;
        }
    }

    /** Send a file to a user */
    /**
     * @param fileName - Full path of the file to download
     * @param downloadFileName - Name that should be given to the file that is downloaded
     */
    void streamFile( HttpServletRequest request, HttpServletResponse response,
                     String fileName, String downloadFileName, String type )
        throws ServletException, IOException
    {
        fileName = BASE_DIRECTORY + "/" + fileName;

        InputStream fileData;

        long length = 0;

        try {
            File file = new File( fileName );
            fileData  = new FileInputStream( file );
            length = file.length();
        } catch ( FileNotFoundException e ) {
            logger.info( "The file '" + fileName + "' does not exist" );
            request.setAttribute( Util.REASON_ATTR, "The file '" + fileName + "' does not exist" );
            rejectFile( request, response );
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

    void rejectFile( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        request.setAttribute( DEBUGGING_ATTR, "" );
        request.setAttribute( VALID_ATTR, false );

        /* Indicate that the response was not rejected */
        response.setStatus( HttpServletResponse.SC_FORBIDDEN );
        request.getRequestDispatcher( "/Index.jsp" ).forward( request, response );
    }

    static Util getInstance()
    {
        return INSTANCE;
    }
}
