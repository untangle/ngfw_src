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

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.AppSettings;
import com.untangle.app.web_filter.WebFilterBase;

@SuppressWarnings("serial")
public class WebFilterUnblockerServlet extends HttpServlet
{

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException
    {
        Map<String,String> i18n_map = UvmContextFactory.context().languageManager().getTranslations( "untangle" );

        response.setContentType("text/xml");
        response.addHeader("Cache-Control", "no-cache");

        String nonce = request.getParameter("nonce");
        String password = request.getParameter("password");
        boolean global = Boolean.parseBoolean(request.getParameter("global"));

        AppManager nm = UvmContextFactory.context().appManager();
        WebFilterBase node = null;
        if ( node == null )
            try {node = (WebFilterBase) nm.app( Long.parseLong(request.getParameter( "tid" )) );} catch (Exception e) {}
        if ( node == null )
            try {node = (WebFilterBase) nm.app( Long.parseLong(request.getParameter( "appid" )) );} catch (Exception e) {}

        try {
            if ( node == null ) { 
                response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr( "App ID not found.", i18n_map ));
                return;
            }
            if ( !(node instanceof WebFilter) ) {
                response.sendError( HttpServletResponse.SC_NOT_ACCEPTABLE, I18nUtil.tr( "Invalid App ID.", i18n_map ));
                return;
            }

            if (node.unblockSite(nonce, global, password)) {
                response.getOutputStream().println("<success/>");
            } else {
                response.getOutputStream().println("<failure/>");
            }
        } catch (IOException exn) {
            throw new ServletException(exn);
        }
    }
}

