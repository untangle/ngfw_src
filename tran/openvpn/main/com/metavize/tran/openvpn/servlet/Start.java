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
import javax.servlet.http.HttpSession;

public class Start extends HttpServlet
{
    protected void service( HttpServletRequest request,  HttpServletResponse response )
        throws ServletException, IOException {
        Util util = Util.getInstance();

        String commonName = util.getCommonName( request );

        if ( commonName == null ) {
            util.rejectFile( request, response );
        } else {
            request.setAttribute( Util.STATUS_ATTR, "READY" );
            request.setAttribute( Util.COMMON_NAME_ATTR, commonName );
            request.getRequestDispatcher("/Index.jsp").forward( request, response );
        }
    }
}
