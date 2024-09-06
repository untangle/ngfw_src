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

public class CaptivePortalSSLEngine
{

    public static final String APP_NAME = "CaptivePortal";
    private final Logger logger = LogManager.getLogger(getClass());
    private final CaptivePortalApp captureApp;
    private AppTCPSession session;
    private SSLContext sslContext;
    private SSLEngine sslEngine;
    private String sniHostname;
    private String appStr;

    // these are used while extracting the SNI from the SSL ClientHello packet
    private static int TLS_HANDSHAKE = 0x16;
    private static int CLIENT_HELLO = 0x01;
    private static int SERVER_NAME = 0x0000;
    private static int HOST_NAME = 0x00;

    /**
     * The constructor sets up the SSLEngine for communicating with the client.
     * 
     * @param appStr
     *        The appid for this instance of the application
     * @param appPtr
     *        The captive portal application
     */
    protected CaptivePortalSSLEngine(String appStr, CaptivePortalApp appPtr)
    {
        String webCertFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getWebCertificate().replaceAll("\\.pem", "\\.pfx");
        this.appStr = appStr;
        this.captureApp = appPtr;

        FileInputStream webCertFileIS = null;
        try {
            // use the web server certfile and password to init our keystore
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            webCertFileIS = new FileInputStream(webCertFile);
            keyStore.load(webCertFileIS, CertificateManager.CERT_FILE_PASSWORD.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, CertificateManager.CERT_FILE_PASSWORD.toCharArray());

            // pass trust_all_certificates as the trust manager for our
            // engine to prevent the SSLEngine from loading cacerts
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), new TrustManager[] { trust_all_certificates }, null);
            sslEngine = sslContext.createSSLEngine();

            // we're acting like a server so set the appropriate engine flags
            sslEngine.setUseClientMode(false);
            sslEngine.setNeedClientAuth(false);
            sslEngine.setWantClientAuth(false);
        } catch (Exception exn) {
            logger.error("Exception creating CaptivePortalSSLEngine()", exn);
        } finally {
            if (webCertFileIS != null) {
                try {
                    webCertFileIS.close();
                } catch (Exception exn) {
                    logger.error("Exception closing web cert file()", exn);

                }
            }
        }
    }

    /**
     * Main handler for data received from clients.
     * 
     * @param session
     *        The TCP session
     * @param buff
     *        The raw data received from the client
     */
    public void handleClientData(AppTCPSession session, ByteBuffer buff)
    {
        this.session = session;
        boolean success = false;

        // pass the data to the client data worker function
        try {
            success = clientDataWorker(session, buff);
        }

        // catch any exceptions
        catch (Exception exn) {
            logger.debug("Exception calling clientDataWorker", exn);
        }

        // null result means something went haywire
        if (!success) {
            session.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_SSL_ENGINE, null);
            session.resetClient();
            session.resetServer();
            session.release();
        }

        return;
    }

    /**
     * Main worker for data received from clients. When OAuth is active we allow
     * traffic to certain domains required to allow a client to interact with an
     * OAuth provider. We also check for pass rules, and pass all other traffic
     * through the SSLEngine so we can decrypt the client request.
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The raw data received from clients
     * @return True for success, otherwise false
     * @throws Exception
     */
    private boolean clientDataWorker(AppTCPSession session, ByteBuffer data) throws Exception
    {
        boolean allowed = false;
        boolean done = false;
        HandshakeStatus status;

        logger.debug("PARAM_BUFFER = " + data.toString());

        if (sniHostname == null){
            try{
                sniHostname = HttpUtility.extractSniHostname(data.duplicate(), APP_NAME);
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
                logger.debug("Releasing HTTPS OAuth session: " + sniHostname);
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
        if ((rule != null) && (rule.getCapture() == false)) {
            logger.debug("Releasing HTTPS session on rule match: " + rule.getDescription());
            captureApp.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            session.sendDataToServer(data);
            session.release();
            return true;
        }

        // no rule match so log and and proceed with sending back the redirect  
        logger.debug("Doing HTTPS-->HTTP redirect for " + session.getOrigClientAddr().getHostAddress().toString());

        while (!done) {
            status = sslEngine.getHandshakeStatus();
            logger.debug("STATUS = " + status);

            // problems with the external server cert seem to cause one
            // of these to become true during handshake so we just return
            if (sslEngine.isInboundDone()) {
                logger.debug("Unexpected isInboundDone() == TRUE");
                return false;
            }
            if (sslEngine.isOutboundDone()) {
                logger.debug("Unexpected isOutboundDone() == TRUE");
                return false;
            }

            switch (status)
            {
            // should never happen since this will only be returned from
            // a call to wrap or unwrap but we include it to be complete
            case FINISHED:
                logger.error("Unexpected FINISHED in dataHandler loop");
                return false;

                // handle outstanding tasks during handshake
            case NEED_TASK:
                done = doNeedTask(data);
                break;

            // handle unwrap during handshake
            case NEED_UNWRAP:
                done = doNeedUnwrap(data);
                break;

            // handle wrap during handshake
            case NEED_WRAP:
                done = doNeedWrap(data);
                break;

            // handle data when no handshake is in progress
            case NOT_HANDSHAKING:
                done = doNotHandshaking(data);
                break;

            // should never happen but we handle just to be safe
            default:
                logger.error("Unknown SSLEngine status in dataHandler loop");
                return false;
            }
        }

        return done;
    }

    /**
     * Called when SSLEngine status = NEED_TASK
     * 
     * @param data
     *        The buffer to be processed
     * @return False to allow the processing loop to continue
     * @throws Exception
     */
    private boolean doNeedTask(ByteBuffer data) throws Exception
    {
        Runnable runnable;

        // loop and run SSLEngine outstanding tasks
        while ((runnable = sslEngine.getDelegatedTask()) != null) {
            logger.debug("EXEC_TASK " + runnable.toString());
            runnable.run();
        }
        return false;
    }

    /**
     * Called when SSLEngine status = NEED_UNWRAP
     * 
     * @param data
     *        Source buffer for encrypted SSL data
     * @return True when all data has been processed, false when data remains
     *         and additional SSLEngine operations are required
     * @throws Exception
     */
    private boolean doNeedUnwrap(ByteBuffer data) throws Exception
    {
        SSLEngineResult result;
        ByteBuffer target = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());

        // unwrap the argumented data into the engine buffer
        result = sslEngine.unwrap(data, target);
        logger.debug("EXEC_UNWRAP " + result.toString());

        if (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
            // underflow during unwrap means the SSLEngine needs more data
            // but it's also possible it used some of the passed data so we
            // compact the receive buffer and hand it back for more
            data.compact();
            logger.debug("UNDERFLOW_LEFTOVER = " + data.toString());
            session.setClientBuffer(data);
            return true;
        }

        // check for engine problems
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine unwrap fault");

        // if the engine result hasn't changed we need more processing
        if (result.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP) return false;

        // the unwrap call shouldn't produce data during handshake and if
        // that is the case we return null here allowing the loop to continue
        if (result.bytesProduced() == 0) return false;

        // unwrap calls during handshake should never produce data
        throw new Exception("SSLEngine produced unexpected data during handshake unwrap");
    }

    /**
     * Called when SSLEngine status = NEED_WRAP
     * 
     * @param data
     *        Source buffer for unencrypted SSL data
     * @return True when all data has been processed, false when data remains
     *         and additional SSLEngine operations are required
     * @throws Exception
     */
    private boolean doNeedWrap(ByteBuffer data) throws Exception
    {
        SSLEngineResult result;
        ByteBuffer target = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());

        // wrap the argumented data into the engine buffer
        result = sslEngine.wrap(data, target);
        logger.debug("EXEC_WRAP " + result.toString());

        // check for engine problems
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine wrap fault");

        // If we produced something for the other side, send it.
        if (result.bytesProduced() != 0){
            target.flip();
            session.sendDataToClient(target);
        }

        // if the engine result hasn't changed we need more processing
        if (result.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) return false;

        if(data.remaining() > 0 ){
            // More data here almost certainly means we've transitioned to FINISHED/NOT_HANDSHAKING mode
            // and the remainder should be handled there.
            return false;
        }

        return true;
    }

    /**
     * Called when SSLEngine status = NOT_HANDSHAKING
     * 
     * @param data
     *        Source buffer for encrypted SSL data
     * @return True when all data has been processed, false when data remains
     *         and additional SSLEngine operations are required
     * @throws Exception
     */
    private boolean doNotHandshaking(ByteBuffer data) throws Exception
    {
        Token[] response = null;
        SSLEngineResult result = null;
        String methodStr = null;
        String hostStr = null;
        String uriStr = null;
        String vector = new String();
        int top, end;
        ByteBuffer target = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());

        // we call unwrap for all data we receive from the client 
        result = sslEngine.unwrap(data, target);
        logger.debug("EXEC_HANDSHAKING " + result.toString());
        logger.debug("LOCAL_BUFFER = " + target.toString());

        // make sure we get a good status return from the SSL engine
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine unwrap fault");

        // if unwrap doesn't produce any data then we are handshaking and 
        // must return null to let the handshake process continue
        if (result.bytesProduced() == 0) return false;

        // when unwrap finally returns some data it will be the client request
        String request = new String(target.array(), 0, target.position());
        String capital = request.toUpperCase();
        logger.debug("CLIENT REQUEST = " + request);

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
        if ((methodStr == null) || (uriStr == null) | (hostStr == null)) {
            logger.warn("Unable to parse client request: " + request);
            session.resetClient();
            session.resetServer();
            session.release();
            return true;
        }

        // now that we've parsed the client request we create the redirect

        // add all off the parameters needed by the capture handler
        // VERY IMPORTANT - the NONCE value must be a1b2c3d4e5f6 because the
        // handler.py script looks for this special value and uses it to
        // decide between http and https when redirecting to the originally
        // requested page after login.  Yes it's a hack but I didn't want to
        // add an additional form field and risk breaking existing custom pages
        CaptivePortalBlockDetails details = new CaptivePortalBlockDetails( hostStr, uriStr, methodStr, "a1b2c3d4e5f6");
        response = captureApp.generateResponse(details, session);

        for(Token token : response){
            if( token instanceof StatusLine ){
                vector += ((StatusLine) token).getString();
            }else if( token instanceof HeaderToken){
                vector += ((HeaderToken) token).getString();
            }
        }

        logger.debug("CLIENT REPLY = " + vector);

        // pass the reply buffer to the SSL engine wrap function
        ByteBuffer ibuff = ByteBuffer.wrap(vector.getBytes());
        ByteBuffer obuff = ByteBuffer.allocate(32768);
        result = sslEngine.wrap(ibuff, obuff);

        // we are done so we cleanup and release the session
        session.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_SSL_ENGINE, null);
        session.release();

        // return the now encrypted reply buffer back to the client
        obuff.flip();
        session.sendDataToClient(obuff);

        return true;
    }

// THIS IS FOR ECLIPSE - @formatter:off

    /*

    This table describes the structure of the TLS ClientHello message:

    Size   Description
    ----------------------------------------------------------------------
    1      Record Content Type
    2      SSL Version
    2      Record Length 
    1      Handshake Type
    3      Message Length
    2      Client Preferred Version
    4      Client Epoch GMT
    28     28 Random Bytes
    1      Session ID Length
    0+     Session ID Data
    2      Cipher Suites Length
    0+     Cipher Suites Data
    1      Compression Methods Length
    0+     Compression Methods Data
    2      Extensions Length
    0+     Extensions Data

    This is the format of an SSLv2 client hello:

    struct {
        uint16 msg_length;
        uint8 msg_type;
        Version version;
        uint16 cipher_spec_length;
        uint16 session_id_length;
        uint16 challenge_length;
        V2CipherSpec cipher_specs[V2ClientHello.cipher_spec_length];
        opaque session_id[V2ClientHello.session_id_length];
        opaque challenge[V2ClientHello.challenge_length;
    } V2ClientHello;


    We don't bother checking the buffer position or length here since the
    caller uses the buffer underflow exception to know when it needs to wait
    for more data when a full packet has not yet been received.

    */

// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * We only do enough SSL MitM to receive and extract the client request and
     * send back a redirect to the capture page, so we never actually connect to
     * the external server, meaning no cert checking is required.
     */
    private TrustManager trust_all_certificates = new X509TrustManager()
    {
        /**
         * Called to check client trust
         * 
         * @param chain
         *        The certificate chain
         * @param authType
         *        The authentication type
         * @throws CertificateException
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        /**
         * Called to check server trust
         * 
         * @param chain
         *        The certificate chain
         * @param authType
         *        The authentication type
         * @throws CertificateException
         */
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        /**
         * Called to get accepted issuers
         * 
         * @return null to accept all issuers
         */
        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }
    };
}
