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

package com.untangle.node.spyware;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.node.http.BlockPageUtil;
import com.untangle.node.http.UserWhitelistMode;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.RemoteBrandingManager;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.util.I18nUtil;

public class BlockPageServlet extends HttpServlet
{
    // HttpServlet methods ----------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        RemoteBrandingManager bm = uvm.brandingManager();
        LocalNodeManager nm = uvm.localNodeManager();

        Map<String,String> i18n_map = LocalUvmContextFactory.context().
            languageManager().getTranslations( "untangle-node-spyware" );

        Tid tid = new Tid(Long.parseLong(request.getParameter( "tid" )));

        NodeContext nodeContext = nm.nodeContext( tid );
        if ( nodeContext == null ) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, 
                                I18nUtil.tr( "Feature is not installed.", i18n_map ));
            return;
        }

        Object oNode = nodeContext.node();
        SpywareBlockDetails blockDetails = null;
        UserWhitelistMode whitelistMode = null;

        String reason = null;
        if ( !(oNode instanceof Spyware) || ( oNode == null )) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, 
                                I18nUtil.tr( "Feature is not installed.", i18n_map ));
            return;
        }
        Spyware node = (Spyware)oNode;
        String nonce = request.getParameter("nonce");
        
        blockDetails = node.getBlockDetails(nonce);
        if (blockDetails == null) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, 
                                I18nUtil.tr( "This request has expired.", i18n_map ));
            return;
        }
        whitelistMode = node.getUserWhitelistMode();

        SpywareBlockPageParameters params = new SpywareBlockPageParameters(blockDetails, whitelistMode);

        BlockPageUtil.getInstance().handle(request, response, this, params);
    }
    
    private static class SpywareBlockPageParameters implements BlockPageUtil.BlockPageParameters
    {
        private final SpywareBlockDetails blockDetails;
        private final UserWhitelistMode userWhitelistMode;

        public SpywareBlockPageParameters( SpywareBlockDetails blockDetails, UserWhitelistMode userWhitelistMode )
        {
            this.blockDetails = blockDetails;
            this.userWhitelistMode = userWhitelistMode;
        }

        /* This is the name of the node to use when retrieving the I18N bundle */
        public String getI18n()
        {
            return "untangle-node-spyware";
        }
        
        /* Retrieve the page title (in the window bar) of the page */
        public String getPageTitle( RemoteBrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr( "{0} | Spyware Blocker Warning", bm.getCompanyName(), i18n_map);
        }
        
        /* Retrieve the title (top of the pae) of the page */
        public String getTitle( RemoteBrandingManager bm, Map<String,String> i18n_map )
        {
            return "Spyware Blocker";
        }
        
        public String getFooter( RemoteBrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr( "{0} Spyware Blocker", bm.getCompanyName(), i18n_map);
        }
        
        /* Return the name of the script file to load, or null if there is not a script. */
        public String getScriptFile()
        {
            return "spyware.js";
        }

        public String getAdditionalFields(Map<String,String> i18n_map)
        {
            return null;
        }
        
        /* Retrieve the description of why this page was blocked. */
        public String getDescription( RemoteBrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr( "{0}This web page was blocked{1} because it may be designed to steal personal information.", new Object[]{ "<b>","</b>" },
                    i18n_map );
        }
    
        public SpywareBlockDetails getBlockDetails()
        {
            return this.blockDetails;
        }
    
        public UserWhitelistMode getUserWhitelistMode()
        {
            return this.userWhitelistMode;
        }
    }
}
