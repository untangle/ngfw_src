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
import javax.servlet.http.HttpSession;

public class Start extends HttpServlet
{
    protected void service( HttpServletRequest request,  HttpServletResponse response )
        throws ServletException, IOException {
        Util util = Util.getInstance();

        if ( util.requiresSecure( request, response )) return;

        String commonName = util.getCommonName( this, request );

        if ( commonName == null ) {
            util.rejectFile( request, response );
        } else {
            request.setAttribute( Util.DEBUGGING_ATTR, "" );
            request.setAttribute( Util.VALID_ATTR, true );
            request.setAttribute( Util.COMMON_NAME_ATTR, commonName );
            request.getRequestDispatcher("/Index.jsp").forward( request, response );
        }
    }
}
