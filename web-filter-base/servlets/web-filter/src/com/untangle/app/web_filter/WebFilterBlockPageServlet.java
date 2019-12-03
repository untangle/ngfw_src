/**
 * $Id$
 */
package com.untangle.app.web_filter;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.app.http.BlockPageUtil;
import com.untangle.uvm.BrandingManager;
import com.untangle.app.web_filter.WebFilterRedirectDetails;
import com.untangle.uvm.util.I18nUtil;

/**
 * Implementation of the Web Filter block page servlet
 */

@SuppressWarnings("serial")
public class WebFilterBlockPageServlet extends com.untangle.app.web_filter.BlockPageServlet
{
    /**
     * Handler for GET requests
     * 
     * @param request
     *        The web request
     * @param response
     *        The server response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        super.doGet(request, response);
    }

    /**
     * Creates parameters used to generate a block page
     * 
     * @param blockDetails
     *        Details about why the session was blocked
     * @param userWhitelistMode
     *        Whitelist mode for the user
     * @return The BlockPageParameters
     */
    protected BlockPageUtil.BlockPageParameters buildBlockPageParameters(WebFilterRedirectDetails blockDetails, String userWhitelistMode)
    {
        return new WebFilterBlockPageParameters(blockDetails, userWhitelistMode);
    }

    /**
     * Generates the parameters used to create a block page
     */
    protected static class WebFilterBlockPageParameters implements BlockPageUtil.BlockPageParameters
    {
        private final WebFilterRedirectDetails blockDetails;
        private final String unblockMode;

        /**
         * Constructor
         * 
         * @param blockDetails
         *        Block details
         * @param unblockMode
         *        Unblock mode
         */
        public WebFilterBlockPageParameters(WebFilterRedirectDetails blockDetails, String unblockMode)
        {
            this.blockDetails = blockDetails;
            this.unblockMode = unblockMode;
        }

        /**
         * Retrieve the page title (in the window bar) of the page
         * 
         * @param bm
         *        Branding manager
         * @param i18n_map
         *        language map
         * @return Page title
         */
        public String getPageTitle(BrandingManager bm, Map<String, String> i18n_map)
        {
            return I18nUtil.tr("{0} | {1} Warning", new String[] { bm.getCompanyName(), this.getAppTitle(bm) }, i18n_map);
        }

        /**
         * Retrieve the title (top of the page) of the page
         * 
         * @param bm
         *        Branding manager
         * @param i18n_map
         *        language map
         * @return Title
         */

        public String getTitle(BrandingManager bm, Map<String, String> i18n_map)
        {
            return this.getAppTitle(bm);
        }

        /**
         * Retrieve the page footer
         * 
         * @param bm
         *        Branding manager
         * @param i18n_map
         *        Language map
         * @return Page footer
         */
        public String getFooter(BrandingManager bm, Map<String, String> i18n_map)
        {
            return bm.getCompanyName() + " " + this.getAppTitle(bm);
        }

        /**
         * Get the name of the script file to load
         * 
         * @return The script file to load or null if there is none
         */
        public String getScriptFile()
        {
            return "web-filter.js";
        }

        /**
         * Get additional fields for the block page
         * 
         * @param i18n_map
         *        Language map
         * @return Optional additional fields for the block page
         */
        public String getAdditionalFields(Map<String, String> i18n_map)
        {
            if ("None".equals(getUnblockMode())) {
                return null;
            }

            if (this.getBlockDetails().getSettings().getUnblockPasswordEnabled() == false) {
                return null;
            }

            String errorText = I18nUtil.tr("The password you entered is incorrect.", i18n_map);
            return "<div class=\"u-form-item\"><label class=\"u-form-item-label\">Password:</label><input class=\"u-form-text u-form-field\" type=\"password\" id=\"unblockPassword\"/></div><div id=\"invalid-password\" style=\"display: none\">" + errorText + "</div>";
        }

        /**
         * Retrieve the description of why this page was blocked.
         * 
         * @param bm
         *        Branding manager
         * @param i18n_map
         *        Language map
         * @return The reason the page was blocked
         */
        public String getDescription(BrandingManager bm, Map<String, String> i18n_map)
        {
            return I18nUtil.tr("{0}This web page is blocked{1} because it violates network policy.", new Object[] { "<b>", "</b>" }, i18n_map);
        }

        /**
         * Gets the block details
         * 
         * @return The block details
         */
        public WebFilterRedirectDetails getBlockDetails()
        {
            return this.blockDetails;
        }

        /**
         * Gets the unblock mode
         * 
         * @return The unblock mode
         */
        public String getUnblockMode()
        {
            return this.unblockMode;
        }

        /**
         * Get the application title
         * 
         * @param bm
         *        Branding manager
         * @return The application title
         */
        private String getAppTitle(BrandingManager bm)
        {
            return this.blockDetails.getAppTitle();
        }
    }
}
