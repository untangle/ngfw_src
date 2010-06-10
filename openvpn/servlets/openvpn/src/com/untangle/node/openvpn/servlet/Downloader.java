/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.openvpn.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.ParseException;

@SuppressWarnings("serial")
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

    private final Logger logger = Logger.getLogger( this.getClass());

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
            try {
                IPaddr address = IPaddr.parse( request.getRemoteAddr());
                util.getNode().addClientDistributionEvent( address, commonName );
            } catch ( NodeException e ) {
                logger.warn( "Unable to log distribution event." );
            } catch ( ParseException e ) {
                logger.warn( "Unable to log distribution event." );
            }
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
