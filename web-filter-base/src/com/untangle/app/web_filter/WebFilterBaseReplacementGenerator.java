/**
 * $Id$
 */

package com.untangle.app.web_filter;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;

/**
 * ReplacementGenerator for WebFilter.
 */
public class WebFilterBaseReplacementGenerator extends ReplacementGenerator<WebFilterRedirectDetails>
{
    private static final HashMap<String,Object> CustomBlockRedirectParameters;
    static {
        CustomBlockRedirectParameters = new HashMap<>();
        CustomBlockRedirectParameters.put("reason", null);
        CustomBlockRedirectParameters.put("appid", null);
        CustomBlockRedirectParameters.put("appname", null);
        CustomBlockRedirectParameters.put("host", null);
        CustomBlockRedirectParameters.put("url", null);
        CustomBlockRedirectParameters.put("clientAddress", null);
    };

    private WebFilterBase webfilterApp = null;

    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This site is blocked because it violates network policy.</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Reason: %s</p>"
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";

    /**
     * Our constuctor
     * 
     * @param appId
     *        The application ID
     * @param app
     *        The application instance that created us
     */
    WebFilterBaseReplacementGenerator(AppSettings appId, WebFilterBase app)
    {
        super(appId);
        this.redirectUri.setPath("/web-filter/blockpage");
        this.webfilterApp = app;
    }

    /**
     * Get the simple replacement page
     * 
     * @param details
     *      The block details
     * @return The replacement
     */
    @Override
    protected String getReplacement(WebFilterRedirectDetails details)
    {
        if(details == null){
            return "";
        }
        UvmContext uvm = UvmContextFactory.context();

        return String.format(BLOCK_TEMPLATE, details.getHeader(),
                             details.getHost(), details.getUri(),
                             details.getReason(),
                             uvm.brandingManager().getContactHtml());
    }

    /**
     * If using a custom block page, use that uri instead of default.
     * @return URIBuilder of uri to redirect client toward.
     */
    protected URIBuilder getRedirectUri()
    {
        if(webfilterApp.getSettings().getCustomBlockPageEnabled()){
            URIBuilder redirectUri = null;
            try{
                redirectUri = new URIBuilder();
                redirectUri = new URIBuilder(webfilterApp.getSettings().getCustomBlockPageUrl());
            }catch(Exception e){}
            return redirectUri;
        }else{
            return super.getRedirectUri();
        }
    }

    /**
     * If using a custom block page, use set of custom parameters instead of defaults.
     * @return New copy of map for parameters.
     */
    protected Map<String,Object> getRedirectParameters()
    {
        if(webfilterApp.getSettings().getCustomBlockPageEnabled()){
            return new HashMap<String,Object>(CustomBlockRedirectParameters);
        }else{
            return super.getRedirectParameters();
        }
    }

}
