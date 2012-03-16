/*
 * $HeadURL: svn://chef/work/src/uvm/servlets/setup/src/com/untangle/uvm/setup/servlet/Language.java $
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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.MarshallException;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;

import com.untangle.uvm.LanguageManager;

/**
 * A servlet which will display the start page
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
@SuppressWarnings("serial")
public class LanguageNew extends HttpServlet
{
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        UvmContext context = UvmContextFactory.context();
        request.setAttribute( "ss", context.skinManager().getSkinSettings());
        request.setAttribute( "timezone", context.adminManager().getTimeZone());

        /* Retrieve the list of languages and serialize it for the setup wizard. */

        JSONSerializer js = new JSONSerializer();

        try {
            js.registerDefaultSerializers();
        } catch ( Exception e ) {
            throw new ServletException( "Unable to load the default serializer", e );
        }

        LanguageManager rlm = context.languageManager();
        
        try {
            request.setAttribute( "languageList", js.toJSON( rlm.getLanguagesList()));
            request.setAttribute( "language", rlm.getLanguageSettings().getLanguage());
        } catch ( MarshallException e ) {
            throw new ServletException( "Unable to serializer JSON", e );
        }
            
        String url="/WEB-INF/jsp/languageNew.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        rd.forward(request, response);
    }
}
