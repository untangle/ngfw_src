/**
 * $Id$
 */
package com.untangle.node.phish;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.node.http.BlockPageUtil;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.NodeSettings;
import com.untangle.uvm.util.I18nUtil;

@SuppressWarnings("serial")
public class BlockPageServlet extends HttpServlet
{
    // HttpServlet methods ----------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        UvmContext uvm = UvmContextFactory.context();
        NodeManager nm = uvm.nodeManager();

        Map<String,String> i18n_map = UvmContextFactory.context().
            languageManager().getTranslations( "untangle-node-phish" );

        NodeContext nodeContext = nm.nodeContext( Long.parseLong(request.getParameter( "tid" )) );

        Object oNode = nodeContext.node();
        PhishBlockDetails blockDetails = null;
        String unblockMode = null;

        if ( !(oNode instanceof Phish) || ( oNode == null )) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, 
                                I18nUtil.tr( "Feature is not installed.", i18n_map ));
            return;
        }
        Phish node = (Phish)oNode;
        String nonce = request.getParameter("nonce");
        
        blockDetails = node.getBlockDetails(nonce);
        if (blockDetails == null) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, 
                                I18nUtil.tr( "Feature is not installed.", i18n_map ));
            return;
        }

        unblockMode = node.getUnblockMode();

        PhishBlockPageParameters params = new PhishBlockPageParameters(blockDetails, unblockMode);

        BlockPageUtil.getInstance().handle(request, response, this, params);
    }
    
    private static class PhishBlockPageParameters implements BlockPageUtil.BlockPageParameters
    {
        private final PhishBlockDetails blockDetails;
        private final String unblockMode;

        public PhishBlockPageParameters( PhishBlockDetails blockDetails, String unblockMode )
        {
            this.blockDetails = blockDetails;
            this.unblockMode = unblockMode;
        }

        /* This is the name of the node to use when retrieving the I18N bundle */
        public String getI18n()
        {
            return "untangle-node-phish";
        }
        
        /* Retrieve the page title (in the window bar) of the page */
        public String getPageTitle( BrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0} | Phish Blocker Warning", bm.getCompanyName(), i18n_map);
        }
        
        /* Retrieve the title (top of the pae) of the page */
        public String getTitle( BrandingManager bm, Map<String,String> i18n_map )
        {
            return "Phish Blocker";
        }
        
        public String getFooter( BrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0}  Phish Blocker", bm.getCompanyName(), i18n_map);
        }
        
        /* Return the name of the script file to load, or null if there is not a script. */
        public String getScriptFile()
        {
            return "blockpage.js";
        }

        public String getAdditionalFields(Map<String,String> i18n_map)
        {
            return null;
        }
        
        /* Retrieve the description of why this page was blocked. */
        public String getDescription( BrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0}Warning - Suspected phishing page.{1} This page may be a forgery or imitation of another website, designed to trick users into sharing personal or financial information. Entering any personal information on this page may result in identity theft or other abuse. ", new Object[]{ "<b>","</b>" },i18n_map );
        }
    
        public PhishBlockDetails getBlockDetails()
        {
            return this.blockDetails;
        }
    
        public String getUnblockMode()
        {
            return this.unblockMode;
        }
    }
}

