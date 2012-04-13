package com.untangle.node.webfilter;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.node.http.BlockPageUtil;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.util.I18nUtil;

@SuppressWarnings("serial")
public class BlockPageServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        UvmContext uvm = UvmContextFactory.context();
        NodeManager nm = uvm.nodeManager();

        Map<String,String> i18n_map = UvmContextFactory.context().languageManager().getTranslations( "untangle-base-webfilter" );

        NodeContext nodeContext = nm.nodeContext( Long.parseLong(request.getParameter( "tid" )) );
        if ( nodeContext == null ) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr( "Feature is not installed.", i18n_map ));
            return;
        }

        WebFilterBlockDetails blockDetails = null;

        Object oNode = nodeContext.node();
        if ( !(oNode instanceof WebFilter) || ( oNode == null )) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr( "Feature is not installed.", i18n_map ));
            return;
        }
        WebFilter node = (WebFilter)oNode;
        String nonce = request.getParameter("nonce");

        blockDetails = node.getDetails(nonce);
        if (blockDetails == null) {
            response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr( "This request has expired.", i18n_map ));
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

        /* This is the name of the node to use when retrieving the I18N bundle */
        public String getI18n()
        {
            return "untangle-base-webfilter";
        }

        /* Retrieve the page title (in the window bar) of the page */
        public String getPageTitle( BrandingManager bm, Map<String,String> i18n_map )
        {
            return I18nUtil.tr("{0} | {1} Warning", new String[] { bm.getCompanyName(), this.blockDetails.getNodeTitle() }, i18n_map);
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
            return "webfilter.js";
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
