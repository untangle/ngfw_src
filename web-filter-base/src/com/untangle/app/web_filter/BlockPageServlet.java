/**
 * $Id$
 */
package com.untangle.app.web_filter;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.app.http.BlockPageUtil;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.util.I18nUtil;

@SuppressWarnings("serial")
public class BlockPageServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        UvmContext uvm = UvmContextFactory.context();
        AppManager nm = uvm.appManager();

        Map<String,String> i18n_map = UvmContextFactory.context().languageManager().getTranslations( "untangle" );

        WebFilter node = null;
        if ( node == null )
            try {node = (WebFilter) nm.app( Long.parseLong(request.getParameter( "tid" )) );} catch (Exception e) {}
        if ( node == null )
            try {node = (WebFilter) nm.app( Long.parseLong(request.getParameter( "appid" )) );} catch (Exception e) {}
            
        if ( node == null ) { 
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr( "App ID not found.", i18n_map ));
            return;
        }
        if ( !(node instanceof WebFilter) ) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr( "Invalid App ID.", i18n_map ));
            return;
        }

        WebFilterBlockDetails blockDetails = null;
        String nonce = request.getParameter("nonce");

        blockDetails = node.getDetails(nonce);
        if (blockDetails == null) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr( "Invalid nonce.", i18n_map ));
            return;
        }

        String unblockMode = node.getSettings().getUnblockMode();

        request.setAttribute( "reason", blockDetails.getReason());
        BlockPageUtil.BlockPageParameters params = this.buildBlockPageParameters(blockDetails, unblockMode);

        BlockPageUtil.getInstance().handle(request, response, this, params);
    }

    protected BlockPageUtil.BlockPageParameters buildBlockPageParameters( WebFilterBlockDetails blockDetails, String unblockMode )
    {
        return new WebFilterBlockPageParameters(blockDetails,unblockMode);
    }

    protected static class WebFilterBlockPageParameters implements BlockPageUtil.BlockPageParameters
    {
        private final WebFilterBlockDetails blockDetails;
        private final String unblockMode;

        public WebFilterBlockPageParameters( WebFilterBlockDetails blockDetails, String unblockMode )
        {
            this.blockDetails = blockDetails;
            this.unblockMode = unblockMode;
        }

        /* Retrieve the page title (in the window bar) of the page */
        public String getPageTitle( BrandingManager bm, Map<String,String> i18n_map )
        {
            return bm.getCompanyName() + " | " + this.blockDetails.getNodeTitle() + " " + I18nUtil.tr("Warning", i18n_map);
        }

        /* Retrieve the title (top of the pae) of the page */
        public String getTitle( BrandingManager bm, Map<String,String> i18n_map )
        {
            return this.blockDetails.getNodeTitle();
        }

        public String getFooter( BrandingManager bm, Map<String,String> i18n_map )
        {
            return bm.getCompanyName() + " " + this.blockDetails.getNodeTitle();
        }

        /* Return the name of the script file to load, or null if there is not a script. */
        public String getScriptFile()
        {
            return "web_filter.js";
        }

        public String getAdditionalFields( Map<String,String> i18n_map )
        {
            return null;
        }

        /* Retrieve the description of why this page was blocked. */
        public String getDescription( BrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0}This web page is blocked{1} because it violates network policy.", new Object[]{ "<b>","</b>" }, i18n_map );
        }

        public WebFilterBlockDetails getBlockDetails()
        {
            return this.blockDetails;
        }

        public String getUnblockMode()
        {
            return this.unblockMode;
        }
    }
}
