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

package com.untangle.node.virus;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import com.untangle.uvm.BrandingBaseSettings;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;

import  com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.util.I18nUtil;

import com.untangle.node.http.BlockPageUtil;  
import com.untangle.node.http.UserWhitelistMode;
import com.untangle.node.virus.VirusBlockDetails;
import com.untangle.node.virus.VirusNodeImpl;

public class BlockPageServlet extends HttpServlet
{
    // HttpServlet methods ----------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        BrandingBaseSettings bs = uvm.brandingManager().getBaseSettings();
        LocalNodeManager nm = uvm.nodeManager();
        
        Tid tid = new Tid(Long.parseLong(request.getParameter( "tid" )));
        
        VirusNodeImpl node = (VirusNodeImpl)nm.nodeContext( tid ).node();
        String nonce = request.getParameter("nonce");

        VirusBlockDetails blockDetails = node.getDetails(nonce);
        request.setAttribute( "reason", blockDetails.getReason());
        VirusHandler handler = new VirusHandler( blockDetails );
                                                         
        BlockPageUtil.getInstance().handle( request, response, this, handler );        
    }
    
    private static class VirusHandler implements BlockPageUtil.Handler
    {
        private final VirusBlockDetails blockDetails;

        public VirusHandler( VirusBlockDetails blockDetails )
        {
            this.blockDetails = blockDetails;
        }

        /* This is the name of the node to use when retrieving the I18N bundle */
        public String getI18n()
        {
            return "untangle-base-virus";
        }
        
        /* Retrieve the page title (in the window bar) of the page */
        public String getPageTitle( BrandingBaseSettings bs, Map<String,String> i18n_map )
        {
            return I18nUtil.tr(i18n_map, "{0} | {1} Warning", 
                    new Object[]{bs.getCompanyName(), this.blockDetails.getVendor()});
        }
        
        /* Retrieve the title (top of the pae) of the page */
        public String getTitle( BrandingBaseSettings bs, Map<String,String> i18n_map )
        {
            return "Virus Blocker";
        }
        
        public String getFooter( BrandingBaseSettings bs, Map<String,String> i18n_map )
        {
            return I18nUtil.tr(i18n_map, "{0} Virus Blocker", bs.getCompanyName());
        }
        
        /* Return the name of the script file to load, or null if there is not a script. */
        public String getScriptFile()
        {
            return null;
        }
        
        /* Retrieve the description of why this page was blocked. */
        public String getDescription( BrandingBaseSettings bs, Map<String,String> i18n_map )
        {
            return I18nUtil.tr(i18n_map, "{0}This file was blocked{1} because it contained a virus.",
                    new Object[]{ "<b>","</b>" } );
        }
    
        public VirusBlockDetails getBlockDetails()
        {
            return this.blockDetails;
        }
    
        public UserWhitelistMode getUserWhitelistMode()
        {
            return UserWhitelistMode.NONE;
        }
    }
}
