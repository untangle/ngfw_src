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
import javax.servlet.http.HttpSession;

@SuppressWarnings("serial")
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
