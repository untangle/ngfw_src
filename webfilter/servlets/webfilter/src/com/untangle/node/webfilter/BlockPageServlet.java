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

package com.untangle.node.webfilter;

import java.io.IOException;
import javax.security.auth.login.FailedLoginException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import com.untangle.uvm.BrandingSettings;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;

import  com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.security.Tid;
  
import com.untangle.node.http.UserWhitelistMode;
import com.untangle.node.webfilter.WebFilter;
import com.untangle.node.webfilter.WebFilterBlockDetails;

public class BlockPageServlet extends HttpServlet
{
    // HttpServlet methods ----------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        BrandingSettings bs = uvm.brandingManager().getBrandingSettings();
        LocalNodeManager nm = uvm.nodeManager();
        
        Tid tid = new Tid(Long.parseLong(request.getParameter( "tid" )));
        
        NodeContext nctx = nm.nodeContext( tid );
        WebFilter tran = (WebFilter)nctx.node();
        UserWhitelistMode mode = tran.getUserWhitelistMode();
        String nonce = request.getParameter("nonce");
        WebFilterBlockDetails bd = tran.getDetails(nonce);
        
        I18n i18n = I18nFactory.getI18n("webfilter", "Messages", Thread.currentThread().getContextClassLoader(), request.getLocale(), I18nFactory.FALLBACK);
        request.setAttribute( "i18n", i18n );

        String company = bs.getCompanyName();

        /* These have to be registered against the request, otherwise
         * the included template cannot see them. */
        request.setAttribute( "bs", bs );
        request.setAttribute( "pageTitle", i18n.tr( "{0} | Web Filter Warning", company ));
        request.setAttribute( "title", "Web Filter" );
        request.setAttribute( "footer", i18n.tr( "{0} Web Filter", company ));
        request.setAttribute( "javascript_file", "webfilter.js" );
        request.setAttribute( "description", i18n.tr( "This web page was blocked because it is considered inappropriate." ));
                     
        /* Register the block detail with the page */
        request.setAttribute( "bd", bd );
        request.setAttribute( "reason", bd.getReason());

        /* Everything below here is abstractable, + i18n should go into an interface */
        request.setAttribute( "url", null == bd ? "javascript:history.back()" : "'" + bd.getFormattedUrl() + "'" );

        /* This is just plain wrong. */
        request.setAttribute( "cdata_start", "<![CDATA" + "[" );
        request.setAttribute( "cdata_end", "]" + "]>" );
        request.setAttribute( "contact", i18n.tr( "Please contact {0}.", bs.getContactHtml()));

        if (UserWhitelistMode.NONE != mode && null != bd && null != bd.getWhitelistHost()) {
            request.setAttribute( "showUnblockNow", true );
            if (UserWhitelistMode.USER_AND_GLOBAL == mode) {
                request.setAttribute( "showUnblockGlobal", true );
            }
        }

        try {
            getServletConfig().getServletContext().getContext( "/" ).
                getRequestDispatcher("/blockpage_template.jspx").forward( request, response );
        } catch ( IOException e ) {
            throw new ServletException( "Unable to render blockpage template.", e );
        }
    }

}
