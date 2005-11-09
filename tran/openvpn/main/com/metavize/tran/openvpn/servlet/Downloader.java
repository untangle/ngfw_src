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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;

public class Downloader extends HttpServlet
{
    private static final String CONFIG_PAGE        = "/config.zip";
    private static final String CONFIG_DOWNLOAD    = "config.zip";
    private static final String CONFIG_NAME_PREFIX = "config-";
    private static final String CONFIG_NAME_SUFFIX = ".zip";

    private static final String SETUP_PAGE        = "/setup.exe";
    private static final String SETUP_DOWNLOAD    = "setup.exe";
    private static final String SETUP_NAME_PREFIX = "setup-";
    private static final String SETUP_NAME_SUFFIX = ".exe";
   

    protected void service( HttpServletRequest request,  HttpServletResponse response )
        throws ServletException, IOException {

        Util util = Util.getInstance();
        String commonName = util.getCommonName( request );
        String fileName = null;
        String download = null;
        String pageName = request.getServletPath();

        if ( pageName.equalsIgnoreCase( CONFIG_PAGE )) {
            fileName = getConfigFileName( commonName );
            download = CONFIG_DOWNLOAD;
        } else if ( pageName.equalsIgnoreCase( SETUP_PAGE )) {
            fileName = getSetupFileName( commonName );
            download = SETUP_DOWNLOAD;
        } else {
            fileName = null;
            download = null;
        }
        
        /* File name shouldn't be null unless the web.xml is misconfigured to force pages
         * that are not supposed to reach here */
        if (( null == commonName ) || ( null == fileName ) || ( null == download )) {
            util.rejectFile( request, response );
        } else {
            util.downloadFile( request, response, fileName, download );
        }
    }

    private String getConfigFileName( String commonName )
    {
        return CONFIG_NAME_PREFIX + commonName + CONFIG_NAME_SUFFIX;
    }

    private String getSetupFileName( String commonName )
    {
        return SETUP_NAME_PREFIX + commonName + SETUP_NAME_SUFFIX;
    }
}
