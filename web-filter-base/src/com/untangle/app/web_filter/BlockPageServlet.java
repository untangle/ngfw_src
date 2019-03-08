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
import com.untangle.uvm.util.I18nUtil;

/**
 * Block page servlet
 */
@SuppressWarnings("serial")
public class BlockPageServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Handle GET requests
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
        UvmContext uvm = UvmContextFactory.context();
        AppManager nm = uvm.appManager();

        Map<String, String> i18n_map = UvmContextFactory.context().languageManager().getTranslations("untangle");

        WebFilter app = null;
        if (app == null) try {
            app = (WebFilter) nm.app(Long.parseLong(request.getParameter("tid")));
        } catch (Exception e) {
        }
        if (app == null) try {
            app = (WebFilter) nm.app(Long.parseLong(request.getParameter("appid")));
        } catch (Exception e) {
        }

        if (app == null) {
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr("App ID not found.", i18n_map));
            return;
        }
        if (!(app instanceof WebFilter)) {
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr("Invalid App ID.", i18n_map));
            return;
        }

        WebFilterBlockDetails blockDetails = null;
        String nonce = request.getParameter("nonce");

        blockDetails = app.getDetails(nonce);
        if (blockDetails == null) {
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr("Invalid nonce.", i18n_map));
            return;
        }

        String unblockMode = app.getSettings().getUnblockMode();

        request.setAttribute("reason", blockDetails.getReason());
        BlockPageUtil.BlockPageParameters params = this.buildBlockPageParameters(blockDetails, unblockMode);

        BlockPageUtil.getInstance().handle(request, response, this, params);
    }

    /**
     * Creates parameters used to generate a block page
     * 
     * @param blockDetails
     *        Details about why the session was blocked
     * @param unblockMode
     *        The unblock mode
     * @return The BlockPageParameters
     */
    protected BlockPageUtil.BlockPageParameters buildBlockPageParameters(WebFilterBlockDetails blockDetails, String unblockMode)
    {
        return new WebFilterBlockPageParameters(blockDetails, unblockMode);
    }

    /**
     * Generates the parameters used to create a block page
     */
    protected static class WebFilterBlockPageParameters implements BlockPageUtil.BlockPageParameters
    {
        private final WebFilterBlockDetails blockDetails;
        private final String unblockMode;

        /**
         * Constructor
         * 
         * @param blockDetails
         *        Block details
         * @param unblockMode
         *        Unblock mode
         */
        public WebFilterBlockPageParameters(WebFilterBlockDetails blockDetails, String unblockMode)
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
            return bm.getCompanyName() + " | " + this.blockDetails.getAppTitle() + " " + I18nUtil.tr("Warning", i18n_map);
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
            return this.blockDetails.getAppTitle();
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
            return bm.getCompanyName() + " " + this.blockDetails.getAppTitle();
        }

        /**
         * Get the name of the script file to load
         * 
         * @return The script file to load or null if there is none
         */
        public String getScriptFile()
        {
            return "web_filter.js";
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
            return null;
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
        public WebFilterBlockDetails getBlockDetails()
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
    }
}
