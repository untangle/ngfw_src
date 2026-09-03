/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

import java.util.Map;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This is the implementation of the replacement generator that creates the HTTP
 * redirect which is sent to unauthenticated clients to redirect them to the
 * captive portal login page.
 *
 * @author mahotz
 *
 */

class CaptivePortalReplacementGenerator extends ReplacementGenerator<CaptivePortalBlockDetails>
{
    private final Logger logger = LogManager.getLogger(getClass());
    private final CaptivePortalApp captureApp;

// THIS IS FOR ECLIPSE - @formatter:off

    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>\r\n"
        + "<TITLE>Captive Portal - Access Denied - Authentication Required</TITLE>\r\n"
        + "</HEAD><BODY>\r\n"
        + "<P><H2><HR><BR><CENTER>This site is blocked because your computer has not been authenticated.</CENTER><BR><HR></H2></P>\r\n"
        + "<P><H3>Request: %s</H3></P>"
        + "<P><H3>Host: %s</H3></P>"
        + "<P><H3>URI: %s</H3></P>"
        + "<P><H3>Please contact %s for assistance.</H3></P>"
        + "</BODY></HTML>";

// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * Our constuctor
     *
     * @param appId
     *        The application ID
     * @param app
     *        The application instance that created us
     */
    CaptivePortalReplacementGenerator(AppSettings appId, CaptivePortalApp app)
    {
        super(appId);
        this.captureApp = app;

        this.redirectUri.setPath("/capture/handler.py/index");

        redirectParameters.put("method", null);
        redirectParameters.put("host", null);
        redirectParameters.put("uri", null);
    }

    /**
     * The replacement to be returned for captured sessions.
     *
     * @param details
     *        The block details
     * @return The replacement to return to the client
     */
    @Override
    protected String getReplacement(CaptivePortalBlockDetails details)
    {
        UvmContext uvm = UvmContextFactory.context();

        logger.debug("getReplacement DETAILS:" + details.toString());

        return String.format(BLOCK_TEMPLATE, details.getMethod(), details.getHost(), details.getUri(), uvm.brandingManager().getContactHtml());
    }

    /**
     * getRedirectUri for the details using the redirectUrl and redirectParams
     * @param redirectDetails
     * @param redirectUri
     * @param redirectParameters
     * @return the URL
     */
    protected String buildRedirectUri(CaptivePortalBlockDetails redirectDetails, URIBuilder redirectUri, Map<String,Object> redirectParameters){

        if (captureApp.getSettings().getRedirectUsingHostname() == true) {
            redirectUri.setHost(UvmContextFactory.context().networkManager().getFullyQualifiedHostname());
        }

        // if the redirectUsingHostname flag is set we use the configured
        // hostname otherwise we use the passed host which should be the IP
        // address of the appropriate interface for the client

        if (captureApp.getSettings().getAlwaysUseSecureCapture() == true) {
            // Http is already configured.  Change to https.
            redirectUri.setScheme("https");
            int httpsPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpsPort();
            redirectUri.setPort(httpsPort);
        }

        URIBuilder externalAuthenticationUri = null;
        Map<String,Object> externalAuthenticationParameters = null;
        CaptivePortalSettings.AuthenticationType authenticationType = captureApp.getSettings().getAuthenticationType();

        return super.buildRedirectUri(redirectDetails, redirectUri, redirectParameters);
    }

     /**
     * Fetch nonce required for direct static page
     * 
     * @param host
     *        The host address for the traffic
     * @param uri
     *        The uri for the traffic
     * @param method
     *        The http method for the traffic
     * @return Nonce
     */
    protected String getCaptivePortalParams(String host, String uri, String method){
        CaptivePortalBlockDetails captivePortalBlockDetails =  new CaptivePortalBlockDetails(host, uri, method);
        return super.generateNonce(captivePortalBlockDetails);
    }
}
