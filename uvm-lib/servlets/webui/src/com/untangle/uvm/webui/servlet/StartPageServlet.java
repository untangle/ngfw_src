/*
 * $HeadURL: svn://chef/work/src/uvm-lib/impl/com/untangle/uvm/engine/ReportingManagerImpl.java $
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

package com.untangle.uvm.webui.servlet;

import java.io.IOException;
import java.util.Random;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.RemoteBrandingManager;

/**
 * A servlet which will display the start page
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
public class StartPageServlet extends HttpServlet
{
    /* ??? Perhaps this should live in a global place. */
    private static final int STORE_WINDOW_ID = new Random().nextInt(Integer.MAX_VALUE);

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String url="/WEB-INF/jsp/startPage.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        String companyName = LocalUvmContextFactory.context().brandingManager().getCompanyName();
        boolean isRegistered = LocalUvmContextFactory.context().isRegistered();
        req.setAttribute( "isRegistered", isRegistered ? "true" : "false" );
        req.setAttribute( "companyName", companyName );
        req.setAttribute( "storeWindowId", STORE_WINDOW_ID );
        req.setAttribute( "buildStamp", getServletConfig().getInitParameter("buildStamp") );
        rd.forward(req, resp);
    }

}
