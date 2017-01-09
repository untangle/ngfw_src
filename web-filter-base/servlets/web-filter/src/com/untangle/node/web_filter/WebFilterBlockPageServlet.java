/**
 * $Id$
 */
package com.untangle.node.web_filter;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.node.http.BlockPageUtil;
import com.untangle.uvm.BrandingManager;
import com.untangle.node.web_filter.WebFilterBlockDetails;
import com.untangle.uvm.util.I18nUtil;

@SuppressWarnings("serial")
public class WebFilterBlockPageServlet extends com.untangle.node.web_filter.BlockPageServlet
{
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        super.doGet(request,response);
    }

    protected BlockPageUtil.BlockPageParameters buildBlockPageParameters( WebFilterBlockDetails blockDetails, String userWhitelistMode )
    {
        return new WebFilterBlockPageParameters(blockDetails,userWhitelistMode);
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
            return I18nUtil.tr("{0} | {1} Warning", new String[] { bm.getCompanyName(), this.getNodeTitle(bm) }, i18n_map);
        }

        /* Retrieve the title (top of the pae) of the page */
        public String getTitle( BrandingManager bm, Map<String,String> i18n_map )
        {
            return this.getNodeTitle(bm);
        }

        public String getFooter( BrandingManager bm, Map<String,String> i18n_map )
        {
            return bm.getCompanyName() + " " + this.getNodeTitle(bm);
        }

        /* Return the name of the script file to load, or null if there is not a script. */
        public String getScriptFile()
        {
            return "web-filter.js";
        }

        public String getAdditionalFields( Map<String,String> i18n_map )
        {
            if ( "None".equals(getUnblockMode()) ) {
                return null;
            }

            if ( this.getBlockDetails().getSettings().getUnblockPasswordEnabled() == false ) {
                return null;
            }

            String errorText = I18nUtil.tr("The password you entered is incorrect.", i18n_map);
            return "<div class=\"u-form-item\"><label class=\"u-form-item-label\">Password:</label><input class=\"u-form-text u-form-field\" type=\"password\" id=\"unblockPassword\"/></div><div id=\"invalid-password\" style=\"display: none\">" + errorText + "</div>";
        }

        /* Retrieve the description of why this page was blocked. */
        public String getDescription( BrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0}This web page is blocked{1} because it violates network policy.", new Object[]{ "<b>","</b>" },
                    i18n_map );
        }

        public WebFilterBlockDetails getBlockDetails()
        {
            return this.blockDetails;
        }

        public String getUnblockMode()
        {
            return this.unblockMode;
        }

        private String getNodeTitle ( BrandingManager bm )
        {
            return this.blockDetails.getNodeTitle();
        }
    }
}
