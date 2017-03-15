/**
 * $Id$
 */
package com.untangle.node.virus_blocker;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.AppManager;
import com.untangle.uvm.node.AppSettings;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.node.http.BlockPageUtil;  
import com.untangle.node.virus_blocker.VirusBlockDetails;
import com.untangle.node.virus_blocker.VirusBlockerBaseApp;

@SuppressWarnings("serial")
public class BlockPageServlet extends HttpServlet
{
    // HttpServlet methods ----------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        UvmContext uvm = UvmContextFactory.context();
        AppManager nm = uvm.appManager();

        Map<String,String> i18n_map = UvmContextFactory.context().languageManager().getTranslations( "untangle" );
        
        VirusBlockerBaseApp node = (VirusBlockerBaseApp) nm.app( Long.parseLong(request.getParameter( "tid" )) );
        if ( node == null || !(node instanceof VirusBlockerBaseApp)) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, 
                                I18nUtil.tr( "Feature is not installed.", i18n_map ));
            return;
        }

        String nonce = request.getParameter("nonce");
        VirusBlockDetails blockDetails = node.getDetails(nonce);
        if (blockDetails == null) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, 
                                I18nUtil.tr( "This request has expired.", i18n_map ));
            return;
        }
        request.setAttribute( "reason", blockDetails.getReason());
        VirusBlockPageParameters params = new VirusBlockPageParameters( node.getAppProperties().getDisplayName(), blockDetails );
                                                         
        BlockPageUtil.getInstance().handle( request, response, this, params );        
    }
    
    private static class VirusBlockPageParameters implements BlockPageUtil.BlockPageParameters
    {
        private final VirusBlockDetails blockDetails;
        private final String nodeTitle;

        public VirusBlockPageParameters( String nodeTitle, VirusBlockDetails blockDetails )
        {
            this.nodeTitle = nodeTitle;
            this.blockDetails = blockDetails;
        }

        /* Retrieve the page title (in the window bar) of the page */
        public String getPageTitle( BrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0} | {1} Warning", new Object[]{bm.getCompanyName(), this.nodeTitle}, i18n_map);
        }
        
        /* Retrieve the title (top of the pae) of the page */
        public String getTitle( BrandingManager bm, Map<String,String> i18n_map )
        {
            return this.nodeTitle;
        }
        
        public String getFooter( BrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0} Virus Blocker", bm.getCompanyName(), i18n_map);
        }
        
        /* Return the name of the script file to load, or null if there is not a script. */
        public String getScriptFile()
        {
            return null;
        }

        public String getAdditionalFields(Map<String,String> i18n_map)
        {
            return null;
        }
        
        /* Retrieve the description of why this page was blocked. */
        public String getDescription( BrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0}This file was blocked{1} because it contains a virus.",
                    new Object[]{ "<b>","</b>" }, i18n_map);
        }
    
        public VirusBlockDetails getBlockDetails()
        {
            return this.blockDetails;
        }
    
        public String getUnblockMode()
        {
            return "None";
        }
    }
}
