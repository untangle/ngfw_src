/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.KeyManagerFactory;

import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.io.FileInputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyStore;

import com.untangle.app.http.HeaderToken;
import com.untangle.app.http.HttpUtility;
import com.untangle.app.http.SslEngineBase;
import com.untangle.app.http.StatusLine;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.OAuthDomain;
import com.untangle.uvm.UvmContextFactory;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * We do just enough SSL MitM to receive and extract the request and send back a
 * redirect to the capture page for unauthenticated clients.
 * 
 * @author mahotz
 * 
 */

public class CaptivePortalSSLEngine extends SslEngineBase
{

    private final Logger logger = LogManager.getLogger(getClass());
    private final CaptivePortalApp captureApp;
    private String sniHostname;

    /**
     * The constructor sets up the SSLEngine for communicating with the client.
     * 
     * @param appPtr
     *        The captive portal application
     */
    protected CaptivePortalSSLEngine(CaptivePortalApp appPtr)
    {
        super();
        this.captureApp = appPtr;
    }

    /**
     * Processes a chunk of client data
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The raw data from client
     * @return True if processing was successful, otherwise false
     */
    public boolean clientDataProcessor(AppTCPSession session, ByteBuffer data)
    {
        boolean allowed = false;
        if (sniHostname == null){
            try{
                sniHostname = HttpUtility.extractSniHostname(data.duplicate(),false);
            }catch (Exception exn) {
                // The client is almost certainly sending us a bad TLS packet.
                session.release();
                return true;
            }
        }

        CaptivePortalSettings.AuthenticationType authType = captureApp.getSettings().getAuthenticationType();

        if (sniHostname != null) {
            // attach sniHostname to session just like SSL Inspector for use by rules 
            session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME, sniHostname);

            // check the SNI name against each item in the OAuthConfigList
            for (OAuthDomain item : captureApp.oauthConfigList) {
                // check PROVIDER = all
                if ((item.provider.equals("all")) && ((authType == CaptivePortalSettings.AuthenticationType.GOOGLE) || (authType == CaptivePortalSettings.AuthenticationType.FACEBOOK) || (authType == CaptivePortalSettings.AuthenticationType.MICROSOFT) || (authType == CaptivePortalSettings.AuthenticationType.ANY_OAUTH))) {
                    if (item.match.equals("full") && sniHostname.toLowerCase().equals(item.name)) allowed = true;
                    if (item.match.equals("end") && sniHostname.toLowerCase().endsWith(item.name)) allowed = true;
                }

                // check PROVIDER = google
                if ((item.provider.equals("google")) && ((authType == CaptivePortalSettings.AuthenticationType.GOOGLE) || (authType == CaptivePortalSettings.AuthenticationType.ANY_OAUTH))) {
                    if (item.match.equals("full") && sniHostname.toLowerCase().equals(item.name)) allowed = true;
                    if (item.match.equals("end") && sniHostname.toLowerCase().endsWith(item.name)) allowed = true;
                    //In some case accounts.google.co. is also getting hit after google auth flow
                    if (item.match.equals("end") && sniHostname.toLowerCase().startsWith("accounts.google.co.")) allowed = true;
                }

                // check PROVIDER = facebook
                if ((item.provider.equals("facebook")) && ((authType == CaptivePortalSettings.AuthenticationType.FACEBOOK) || (authType == CaptivePortalSettings.AuthenticationType.ANY_OAUTH))) {
                    if (item.match.equals("full") && sniHostname.toLowerCase().equals(item.name)) allowed = true;
                    if (item.match.equals("end") && sniHostname.toLowerCase().endsWith(item.name)) allowed = true;
                }

                // check PROVIDER = microsoft
                if ((item.provider.equals("microsoft")) && ((authType == CaptivePortalSettings.AuthenticationType.MICROSOFT) || (authType == CaptivePortalSettings.AuthenticationType.ANY_OAUTH))) {
                    if (item.match.equals("full") && sniHostname.toLowerCase().equals(item.name)) allowed = true;
                    if (item.match.equals("end") && sniHostname.toLowerCase().endsWith(item.name)) allowed = true;
                }
            }

            if (allowed) {
                logger.debug("Releasing HTTPS OAuth session: {}", sniHostname);
                session.sendDataToServer(data);
                session.release();
                return true;
            }
        }

        // grab the cached certificate for the server
        X509Certificate serverCert = UvmContextFactory.context().certCacheManager().fetchServerCertificate(session.getServerAddr().getHostAddress().toString());

        // attach the subject and issuer names just like SSL Inspector for use by the rule matcher
        if (serverCert != null) {
            session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_SUBJECT_DN, serverCert.getSubjectX500Principal().toString());
            session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_ISSUER_DN, serverCert.getIssuerX500Principal().toString());
        }

        // do the rule check again now that we have the SSL attachments
        CaptureRule rule = captureApp.checkCaptureRules(session);

        // if we find a pass rule allow the session
        if ((rule != null) && !rule.getCapture()) {
            logger.debug("Releasing HTTPS session on rule match: {}", rule.getDescription());
            captureApp.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            session.sendDataToServer(data);
            session.release();
            return true;
        }

        // no rule match so log and and proceed with sending back the redirect  
        logger.debug("Doing HTTPS-->HTTP redirect for {}", session.getOrigClientAddr().getHostAddress().toString());
        return false;
    }

    /**
     * Generate the response
     * @param request
     *        Client request
     * @param session
     *        The TCP session
     * @return response if request parse successfully else null
     */
    public Token[] generateResponse(String request, AppTCPSession session)
    {
        String methodStr = null;
        String hostStr = null;
        String uriStr = null;
        int top, end;
        String capital = request.toUpperCase();
        // extract the method from the request
        end = request.indexOf(" ");
        if (end >= 0) methodStr = request.substring(0, end);

        // extract the URL from the request
        top = request.indexOf(" ", end);
        end = request.indexOf("HTTP/", top);
        if ((top >= 0) && (end >= 0)) uriStr = new String(request.substring(top + 1, end - 1));

        // extract the destination host from the request
        String look = "HOST: ";
        top = capital.indexOf(look);
        end = capital.indexOf("\r\n", top);
        if ((top >= 0) && (end >= 0)) hostStr = new String(request.substring(top + look.length(), end));

        // if we couldn't parse any of our strings log an error and block
        if ((methodStr == null) || (uriStr == null) || (hostStr == null)) {
            logger.warn("Unable to parse client request: {}", request);
            session.resetClient();
            session.resetServer();
            session.release();
            return null;
        }

        // now that we've parsed the client request we create the redirect

        // add all off the parameters needed by the capture handler
        // VERY IMPORTANT - the NONCE value must be a1b2c3d4e5f6 because the
        // handler.py script looks for this special value and uses it to
        // decide between http and https when redirecting to the originally
        // requested page after login.  Yes it's a hack but I didn't want to
        // add an additional form field and risk breaking existing custom pages
        CaptivePortalBlockDetails details = new CaptivePortalBlockDetails( hostStr, uriStr, methodStr, "a1b2c3d4e5f6");
        return captureApp.generateResponse(details, session);
    }

    /**
     * 
     * @param result
     *        SSL engine result
     * @param data
     *        The raw data from client
     * @return always false as captive portal does not require this check
     */
    public boolean verifySSLstatus(SSLEngineResult result,  ByteBuffer data){
        return false;
    }
}
