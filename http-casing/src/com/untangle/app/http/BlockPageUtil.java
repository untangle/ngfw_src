/**
 * $Id$
 */
package com.untangle.app.http;

import java.io.IOException;
import java.util.Map;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.util.I18nUtil;

public class BlockPageUtil
{
    private static final BlockPageUtil INSTANCE = new BlockPageUtil();
    
    private final Logger logger = Logger.getLogger(this.getClass());

    private BlockPageUtil()
    {
    }

    public void handle(HttpServletRequest request, HttpServletResponse response, HttpServlet servlet, BlockPageParameters params)
        throws ServletException
    {
        UvmContext uvm = UvmContextFactory.context();
        BrandingManager bm = uvm.brandingManager();

        Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle");
        request.setAttribute( "i18n_map", i18n_map );

        /* These have to be registered against the request, otherwise
         * the included template cannot see them. */
        request.setAttribute( "skinName", uvm.skinManager().getSettings().getSkinName());
        request.setAttribute( "pageTitle", params.getPageTitle( bm, i18n_map ));
        request.setAttribute( "title", params.getTitle( bm, i18n_map ));
        request.setAttribute( "footer", params.getFooter( bm, i18n_map ));
        
        String value = params.getScriptFile();
        if ( value != null ) request.setAttribute( "javascript_file", value );
        value = params.getAdditionalFields( i18n_map );
        if ( value != null ) request.setAttribute( "additional_fields", value );
        request.setAttribute( "description", params.getDescription( bm, i18n_map ));

        /* Register the block detail with the page */
        BlockDetails bd = params.getBlockDetails();
        request.setAttribute( "bd", bd );

        String contactHtml = I18nUtil.marktr("your network administrator");
        if (bm.getContactEmail() != null) {
            String emailSubject = "";
            String emailBody = "";
            try {
                emailSubject = URLEncoder.encode( "site:" + bd.getHost(), "UTF-8" );
                emailBody = URLEncoder.encode("URL:http://" + bd.getHost() + bd.getUri(), "UTF-8");
            } catch (java.io.UnsupportedEncodingException exc) {
                logger.warn("unsupported encoding", exc);
            }
            contactHtml = "<a href='mailto:" + bm.getContactEmail() + "?subject=" + emailSubject + "&body=" + emailBody + "'>" + bm.getContactName() + "</a>";
        }

        request.setAttribute( "contact", I18nUtil.tr("If you have any questions, Please contact {0}.", contactHtml, i18n_map));
        request.setAttribute( "companyUrl", bm.getCompanyUrl());
        String mode = (params.getUnblockMode() == null ? "" : params.getUnblockMode()).trim().toLowerCase();

        if ((!BlockPageParameters.UNBLOCK_MODE_NONE.equals(mode)) && ( null != bd ) && ( null != bd.getWhitelistHost())) {
            request.setAttribute( "showUnblockNow", true );
            if (BlockPageParameters.UNBLOCK_MODE_GLOBAL.equals(mode)) {
                request.setAttribute( "showUnblockGlobal", true );
            }
        }

        try {
            servlet.getServletConfig().getServletContext().getContext("/blockpage").getRequestDispatcher("/blockpage_template.jspx").forward(request, response);
        } catch ( IOException e ) {
            logger.warn( "Unable to render blockpage template.", e );
            throw new ServletException( "Unable to render blockpage template.", e );
        }
    }

    public interface BlockPageParameters
    {
        /* Retrieve the page title (in the window bar) of the page */
        public String getPageTitle( BrandingManager bm, Map<String,String> i18n_map );

        /* Retrieve the title (top of the pae) of the page */
        public String getTitle( BrandingManager bm, Map<String,String> i18n_map );

        public String getFooter( BrandingManager bm, Map<String,String> i18n_map );

        /* Return the name of the script file to load, or null if there is not a script. */
        public String getScriptFile();

        /* Return any additional fields that should go on the page. */
        public String getAdditionalFields( Map<String,String> i18n_map );

        /* Retrieve the description of why this page was blocked. */
        public String getDescription( BrandingManager bm, Map<String,String> i18n_map );

        public BlockDetails getBlockDetails();

        public static final String UNBLOCK_MODE_NONE   = "none";
        public static final String UNBLOCK_MODE_HOST   = "host";
        public static final String UNBLOCK_MODE_GLOBAL = "global";
        public String getUnblockMode();
    }

    public static BlockPageUtil getInstance()
    {
        return INSTANCE;
    }
}
