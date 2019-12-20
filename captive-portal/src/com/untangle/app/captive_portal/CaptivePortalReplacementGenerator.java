/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

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
    private final Logger logger = Logger.getLogger(getClass());
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

    protected static final String AUTH_REDIRECT_URI = "https://auth-relay.untangle.com/callback.php";
    
    protected static final String GOOGLE_AUTH_HOST = "accounts.google.com";
    protected static final String GOOGLE_AUTH_PATH = "/o/oauth2/v2/auth";
    protected static final String GOOGLE_CLIENT_ID = "365238258169-6k7k0ett96gv2c8392b9e1gd602i88sr.apps.googleusercontent.com";
    protected static final Map<String,Object> GOOGLE_PARAMETERS;

    protected static final String FACEBOOK_AUTH_HOST = "www.facebook.com";
    protected static final String FACEBOOK_AUTH_PATH = "/v2.9/dialog/oauth";
    protected static final String FACEBOOK_CLIENT_ID = "1840471182948119";
    protected static final Map<String,Object> FACEBOOK_PARAMETERS;

    protected static final String MICROSOFT_AUTH_HOST = "login.microsoftonline.com";
    protected static final String MICROSOFT_AUTH_PATH = "/common/oauth2/v2.0/authorize";
    protected static final String MICROSOFT_CLIENT_ID = "f963a9b1-4d6c-4970-870d-3a75014e1364";
    protected static final Map<String,Object> MICROSOFT_PARAMETERS;

    static {
        GOOGLE_PARAMETERS = new HashMap<>();
        GOOGLE_PARAMETERS.put("client_id", GOOGLE_CLIENT_ID);
        GOOGLE_PARAMETERS.put("redirect_uri", AUTH_REDIRECT_URI);
        GOOGLE_PARAMETERS.put("response_type", "code");
        GOOGLE_PARAMETERS.put("scope", "email");
        GOOGLE_PARAMETERS.put("state", null);

        FACEBOOK_PARAMETERS = new HashMap<>();
        FACEBOOK_PARAMETERS.put("client_id", FACEBOOK_CLIENT_ID);
        FACEBOOK_PARAMETERS.put("redirect_uri", AUTH_REDIRECT_URI);
        FACEBOOK_PARAMETERS.put("response_type", "code");
        FACEBOOK_PARAMETERS.put("scope", "email");
        FACEBOOK_PARAMETERS.put("state", null);

        MICROSOFT_PARAMETERS = new HashMap<>();
        MICROSOFT_PARAMETERS.put("client_id", MICROSOFT_CLIENT_ID);
        MICROSOFT_PARAMETERS.put("redirect_uri", AUTH_REDIRECT_URI);
        MICROSOFT_PARAMETERS.put("response_type", "code");
        MICROSOFT_PARAMETERS.put("scope", "openid User.Read");
        MICROSOFT_PARAMETERS.put("state", null);
    }

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
            if (httpsPort != 443){
                redirectUri.setPort(httpsPort);
            }
        }

        URIBuilder externalAuthenticationUri = null;
        Map<String,Object> externalAuthenticationParameters = null;
        CaptivePortalSettings.AuthenticationType authenticationType = captureApp.getSettings().getAuthenticationType();

        if(authenticationType == CaptivePortalSettings.AuthenticationType.GOOGLE ||
           authenticationType == CaptivePortalSettings.AuthenticationType.FACEBOOK ||
           authenticationType == CaptivePortalSettings.AuthenticationType.MICROSOFT){
            /**
             * Authentication type requires redirect to provider.
             */
            redirectUri.setParameters(buildRedirectParameters(redirectDetails, redirectParameters));
            externalAuthenticationUri = new URIBuilder();
            externalAuthenticationUri.setScheme("https");

            switch(authenticationType){
                case GOOGLE:
                    externalAuthenticationUri.setHost(GOOGLE_AUTH_HOST);
                    externalAuthenticationUri.setPath(GOOGLE_AUTH_PATH);
                    externalAuthenticationParameters = new HashMap<>(GOOGLE_PARAMETERS);
                    break;
                case FACEBOOK:
                    externalAuthenticationUri.setHost(FACEBOOK_AUTH_HOST);
                    externalAuthenticationUri.setPath(FACEBOOK_AUTH_PATH);
                    externalAuthenticationParameters = new HashMap<>(FACEBOOK_PARAMETERS);
                    break;
                case MICROSOFT:
                    externalAuthenticationUri.setHost(MICROSOFT_AUTH_HOST);
                    externalAuthenticationUri.setPath(MICROSOFT_AUTH_PATH);
                    externalAuthenticationParameters = new HashMap<>(MICROSOFT_PARAMETERS);
                    break;
            }
            externalAuthenticationParameters.put("state", redirectUri.toString());
            redirectUri = externalAuthenticationUri;
            redirectParameters = externalAuthenticationParameters;
        }

        return super.buildRedirectUri(redirectDetails, redirectUri, redirectParameters);
    }
}
