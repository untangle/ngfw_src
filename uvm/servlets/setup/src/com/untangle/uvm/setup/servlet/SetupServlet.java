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

import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.json.JSONObject;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.LocalUvmContext;

/**
 * A servlet which will display the start page
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
@SuppressWarnings("serial")
public class SetupServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        LocalUvmContext context = LocalUvmContextFactory.context();
        request.setAttribute( "ss", context.skinManager().getSkinSettings());
        request.setAttribute( "timezone", context.adminManager().getTimeZone());

        Map<String,String> languageMap = context.languageManager().getTranslations( "untangle-libuvm" );
        request.setAttribute( "languageMap", new JSONObject( languageMap ).toString());

        try {
            List<String> il = context.networkManager().getPhysicalInterfaceNames();
            request.setAttribute( "hasMultipleInterfaces", il.size() > 1 );
        } catch ( Exception e ) {
            logger.warn( "Unable to determine the number of interfaces, assuming multiple.", e );
            request.setAttribute( "hasMultipleInterfaces", true );
        }
            
        String url="/WEB-INF/jsp/setup.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        rd.forward(request, response);
    }
}
