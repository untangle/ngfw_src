/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm/servlets/webui/src/com/untangle/uvm/webui/jabsorb/UtJsonRpcServlet.java $
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

package com.untangle.uvm.setup.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.LocalUvmContextFactory;

/**
 * A servlet which will display the start page
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
@SuppressWarnings("serial")
public class Welcome extends HttpServlet
{
    private static final String WEBUI_URL = "/webui/startPage.do";
    private static final String SETUP_URL = "/setup/language.do";
        
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {        
        String url = WEBUI_URL;

        /* If the user is not registered send them to the setup page. */
        if ( !LocalUvmContextFactory.context().isRegistered()) url = SETUP_URL;

        if (request.getParameter("console") != null && request.getParameter("console").equals("1")) {
            url = url + "?console=1";
        }

        response.sendRedirect( url );
    }
}
