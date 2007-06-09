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

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Downloader extends HttpServlet
{
    private static final String CONFIG_PAGE        = "/config.zip";
    private static final String CONFIG_DOWNLOAD    = "config.zip";
    private static final String CONFIG_NAME_PREFIX = "config-";
    private static final String CONFIG_NAME_SUFFIX = ".zip";
    private static final String CONFIG_TYPE        = "application/zip";

    private static final String SETUP_PAGE        = "/setup.exe";
    private static final String SETUP_DOWNLOAD    = "setup.exe";
    private static final String SETUP_NAME_PREFIX = "setup-";
    private static final String SETUP_NAME_SUFFIX = ".exe";
    private static final String SETUP_TYPE        = "application/download";

    protected void service( HttpServletRequest request,  HttpServletResponse response )
        throws ServletException, IOException {

        Util util = Util.getInstance();

        if ( util.requiresSecure( request, response )) return;

        String commonName = util.getCommonName(this, request );
        String fileName = null;
        String download = null;
        String pageName = request.getServletPath();
        String type = "";

        if ( pageName.equalsIgnoreCase( CONFIG_PAGE )) {
            fileName = getConfigFileName( commonName );
            download = CONFIG_DOWNLOAD;
            type     = CONFIG_TYPE;
        } else if ( pageName.equalsIgnoreCase( SETUP_PAGE )) {
            fileName = getSetupFileName( commonName );
            download = SETUP_DOWNLOAD;
            type     = SETUP_TYPE;
        } else {
            fileName = null;
            download = null;
        }

        /* File name shouldn't be null unless the web.xml is misconfigured to force pages
         * that are not supposed to reach here */
        if (( null == commonName ) || ( null == fileName ) || ( null == download )) {
            if ( commonName != null ) {
                request.setAttribute( Util.REASON_ATTR, "download or fileName is null [" + pageName + "]" );
            }
            util.rejectFile( request, response );
        } else {
            util.streamFile( request, response, fileName, download, type );
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
