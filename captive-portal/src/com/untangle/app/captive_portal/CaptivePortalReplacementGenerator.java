/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

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
    protected static final String GOOGLE_CLIENT_ID = "271473271217-m3kh6o0coa3kfb515un8gkhrdako9acv.apps.googleusercontent.com";

    protected static final String FACEBOOK_AUTH_HOST = "www.facebook.com";
    protected static final String FACEBOOK_AUTH_PATH = "/v2.9/dialog/oauth";
    protected static final String FACEBOOK_CLIENT_ID = "1840471182948119";

    protected static final String MICROSOFT_AUTH_HOST = "login.microsoftonline.com";
    protected static final String MICROSOFT_AUTH_PATH = "/common/oauth2/v2.0/authorize";
    protected static final String MICROSOFT_CLIENT_ID = "f963a9b1-4d6c-4970-870d-3a75014e1364";

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
     * Creates an HTTP redirect to the login page for unauthenticated clients.
     * 
     * @param nonce
     *        A random nonce used to prevent issues with browser caching
     * @param host
     *        The IP used by the client to reach this server
     * @param appSettings
     *        The settings object for the associated application instance
     * @return
     */
    @Override
    protected String getRedirectUrl(String nonce, String host, AppSettings appSettings)
    {
        URIBuilder target = new URIBuilder();
        URIBuilder exauth = new URIBuilder();
        int httpPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();
        int httpsPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpsPort();

        CaptivePortalBlockDetails details = getNonceData(nonce);
        logger.debug("getRedirectUrl " + details.toString());

        // if the redirectUsingHostname flag is set we use the configured
        // hostname otherwise we use the passed host which should be the IP
        // address of the appropriate interface for the client
        if (captureApp.getSettings().getRedirectUsingHostname() == true) {
            target.setHost(UvmContextFactory.context().networkManager().getFullyQualifiedHostname());
        } else {
            target.setHost(host.split(":")[0]);
        }

        // set the path of the capture handler
        target.setPath("/capture/handler.py/index");

        // set the scheme and port appropriately
        if (captureApp.getSettings().getAlwaysUseSecureCapture() == true) {
            target.setScheme("https");
            if (httpsPort != 443) target.setPort(httpsPort);
        } else {
            target.setScheme("http");
            if (httpPort != 80) target.setPort(httpPort);
        }

        // add all off the parameters needed by the capture handler
        target.addParameter("nonce", nonce);
        target.addParameter("method", details.getMethod());
        target.addParameter("appid", Long.toString(appSettings.getId()));
        target.addParameter("host", details.getHost());
        target.addParameter("uri", details.getUri());

        // if using Google authentication setup the authentication redirect
        // and pass the target as the OAuth state
        if (captureApp.getSettings().getAuthenticationType() == CaptivePortalSettings.AuthenticationType.GOOGLE) {
            exauth.setScheme("https");
            exauth.setHost(GOOGLE_AUTH_HOST);
            exauth.setPath(GOOGLE_AUTH_PATH);
            exauth.addParameter("client_id", GOOGLE_CLIENT_ID);
            exauth.addParameter("redirect_uri", AUTH_REDIRECT_URI);
            exauth.addParameter("response_type", "code");
            exauth.addParameter("scope", "email");
            exauth.addParameter("state", target.toString());
            logger.debug("CLIENT REPLY = " + exauth.toString());
            return (exauth.toString());
        }

        // if using Facebook authentication setup the authentication redirect
        // and pass the target as the OAuth state
        if (captureApp.getSettings().getAuthenticationType() == CaptivePortalSettings.AuthenticationType.FACEBOOK) {
            exauth.setScheme("https");
            exauth.setHost(FACEBOOK_AUTH_HOST);
            exauth.setPath(FACEBOOK_AUTH_PATH);
            exauth.addParameter("client_id", FACEBOOK_CLIENT_ID);
            exauth.addParameter("redirect_uri", AUTH_REDIRECT_URI);
            exauth.addParameter("response_type", "code");
            exauth.addParameter("scope", "email");
            exauth.addParameter("state", target.toString());
            logger.debug("CLIENT REPLY = " + exauth.toString());
            return (exauth.toString());
        }

        // if using Microsoft authentication setup the authentication redirect
        // and pass the target as the OAuth state
        if (captureApp.getSettings().getAuthenticationType() == CaptivePortalSettings.AuthenticationType.MICROSOFT) {
            exauth.setScheme("https");
            exauth.setHost(MICROSOFT_AUTH_HOST);
            exauth.setPath(MICROSOFT_AUTH_PATH);
            exauth.addParameter("client_id", MICROSOFT_CLIENT_ID);
            exauth.addParameter("redirect_uri", AUTH_REDIRECT_URI);
            exauth.addParameter("response_type", "code");
            exauth.addParameter("scope", "openid User.Read");
            exauth.addParameter("state", target.toString());
            logger.debug("CLIENT REPLY = " + exauth.toString());
            return (exauth.toString());
        }

        // local authentication so return the target directly
        logger.debug("CLIENT REPLY = " + target);
        return (target.toString());
    }
}
