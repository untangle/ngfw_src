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
package com.untangle.node.http;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.BrandingBaseSettings;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.I18nUtil;

public class BlockPageUtil
{
    private static final BlockPageUtil INSTANCE = new BlockPageUtil();

    private BlockPageUtil()
    {
    }

    public void handle(HttpServletRequest request, HttpServletResponse response,
                       HttpServlet servlet, Handler handler)
        throws ServletException
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        BrandingBaseSettings bs = uvm.brandingManager().getBaseSettings();

        String module = handler.getI18n();
        Map<String,String> i18n_map = uvm.languageManager().getTranslations(module);
        request.setAttribute( "i18n_map", i18n_map );

        /* These have to be registered against the request, otherwise
         * the included template cannot see them. */
        request.setAttribute( "ss", uvm.skinManager().getSkinSettings());
        request.setAttribute( "bs", bs );
        request.setAttribute( "pageTitle", handler.getPageTitle( bs, i18n_map ));
        request.setAttribute( "title", handler.getTitle( bs, i18n_map ));
        request.setAttribute( "footer", handler.getFooter( bs, i18n_map ));
        String value = handler.getScriptFile();
        if ( value != null ) request.setAttribute( "javascript_file", value );
        request.setAttribute( "description", handler.getDescription( bs, i18n_map ));

        /* Register the block detail with the page */
        BlockDetails bd = handler.getBlockDetails();
        request.setAttribute( "bd", bd );

        /* Everything below here can be moved into a parent class, + i18n should go into an interface */
        if ( null == bd ) value = "javascript:history.back()";
        else value = "'" + bd.getFormattedUrl() + "'";
        request.setAttribute( "url", value );

        request.setAttribute( "contact", I18nUtil.tr(i18n_map, "If you have any questions, Please contact {0}.", bs.getContactHtml()));

        UserWhitelistMode mode = handler.getUserWhitelistMode();
        if (( UserWhitelistMode.NONE != mode ) && ( null != bd ) && ( null != bd.getWhitelistHost())) {
            request.setAttribute( "showUnblockNow", true );
            if (UserWhitelistMode.USER_AND_GLOBAL == mode) {
                request.setAttribute( "showUnblockGlobal", true );
            }
        }

        try {
            servlet.getServletConfig().getServletContext()
                .getContext("/blockpage")
                .getRequestDispatcher("/blockpage_template.jspx")
                .forward(request, response);
        } catch ( IOException e ) {
            throw new ServletException( "Unable to render blockpage template.", e );
        }
    }

    public interface Handler
    {
        /* This is the name of the node to use when retrieving the I18N bundle */
        public String getI18n();

        /* Retrieve the page title (in the window bar) of the page */
        public String getPageTitle( BrandingBaseSettings bs, Map<String,String> i18n_map );

        /* Retrieve the title (top of the pae) of the page */
        public String getTitle( BrandingBaseSettings bs, Map<String,String> i18n_map );

        public String getFooter( BrandingBaseSettings bs, Map<String,String> i18n_map );

        /* Return the name of the script file to load, or null if there is not a script. */
        public String getScriptFile();

        /* Retrieve the description of why this page was blocked. */
        public String getDescription( BrandingBaseSettings bs, Map<String,String> i18n_map );

        public BlockDetails getBlockDetails();

        public UserWhitelistMode getUserWhitelistMode();
    }

    public static BlockPageUtil getInstance()
    {
        return INSTANCE;
    }

}
