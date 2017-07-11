/*
 * $Id$
 */

package com.untangle.app.captive_portal;

import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.KeyManagerFactory;

import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.io.FileInputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyStore;

import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.UvmContextFactory;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

public class CaptivePortalSSLEngine
{

    private final Logger logger = Logger.getLogger(getClass());
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

    protected CaptivePortalSSLEngine(String appStr, CaptivePortalApp appPtr)
    {
        String webCertFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getWebCertificate().replaceAll("\\.pem", "\\.pfx");
        this.appStr = appStr;
        this.captureApp = appPtr;

        try {
            // use the web server certfile and password to init our keystore
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(webCertFile), CertificateManager.CERT_FILE_PASSWORD.toCharArray());
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
        }

        catch (Exception exn) {
            logger.error("Exception creating CaptivePortalSSLEngine()", exn);
        }
    }

    // ------------------------------------------------------------------------

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
            logger.debug("Exception calling clilentDataWorker", exn);
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

    // ------------------------------------------------------------------------

    private boolean clientDataWorker(AppTCPSession session, ByteBuffer data) throws Exception
    {
        ByteBuffer target = ByteBuffer.allocate(32768);
        boolean allowed = false;
        boolean done = false;
        HandshakeStatus status;

        logger.debug("PARAM_BUFFER = " + data.toString());

        if (sniHostname == null) sniHostname = extractSNIhostname(data.duplicate());

        if (sniHostname != null) {
            if (sniHostname.toLowerCase().equals("auth-relay.untangle.com")) allowed = true;

            // hosts we must allow for Google OAuth
            if (sniHostname.toLowerCase().equals("accounts.google.com")) allowed = true;
            if (sniHostname.toLowerCase().equals("ssl.gstatic.com")) allowed = true;

            // hosts we must allow for Facebook OAuth
            if (sniHostname.toLowerCase().equals("www.facebook.com")) allowed = true;
            if (sniHostname.toLowerCase().equals("graph.facebook.com")) allowed = true;

            if (allowed) {
                logger.debug("Releasing session: " + sniHostname);
                session.sendDataToServer(data);
                session.release();
                return true;
            }
        }

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
                done = doNeedUnwrap(data, target);
                break;

            // handle wrap during handshake
            case NEED_WRAP:
                done = doNeedWrap(data, target);
                break;

            // handle data when no handshake is in progress
            case NOT_HANDSHAKING:
                done = doNotHandshaking(data, target);
                break;

            // should never happen but we handle just to be safe
            default:
                logger.error("Unknown SSLEngine status in dataHandler loop");
                return false;
            }
        }

        return done;
    }

    // ------------------------------------------------------------------------

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

    // ------------------------------------------------------------------------

    private boolean doNeedUnwrap(ByteBuffer data, ByteBuffer target) throws Exception
    {
        SSLEngineResult result;

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

    // ------------------------------------------------------------------------

    private boolean doNeedWrap(ByteBuffer data, ByteBuffer target) throws Exception
    {
        SSLEngineResult result;

        // wrap the argumented data into the engine buffer
        result = sslEngine.wrap(data, target);
        logger.debug("EXEC_WRAP " + result.toString());

        // check for engine problems
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine wrap fault");

        // if the engine result hasn't changed we need more processing
        if (result.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) return false;

        // if the wrap call didn't produce any data return null
        if (result.bytesProduced() == 0) return false;

        // the wrap call produced some data so return it to the client
        target.flip();
        ByteBuffer array[] = new ByteBuffer[1];
        array[0] = target;
        session.sendDataToClient(array);
        return true;
    }

    // ------------------------------------------------------------------------

    private boolean doNotHandshaking(ByteBuffer data, ByteBuffer target) throws Exception
    {
        SSLEngineResult result = null;
        String methodStr = null;
        String hostStr = null;
        String uriStr = null;
        String vector = null;
        int top, end;

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
        if ((top >= 0) && (end >= 0)) uriStr = new String(request.substring(top + 1, end));

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

        URIBuilder output = new URIBuilder();
        URIBuilder exauth = new URIBuilder();
        InetAddress hostAddr = UvmContextFactory.context().networkManager().getInterfaceHttpAddress(session.getClientIntf());
        int httpPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();
        int httpsPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();

        // if the redirectUsingHostname flag is set we use the configured
        // hostname otherwise we use the address of the client interface 
        if (captureApp.getCaptivePortalSettings().getRedirectUsingHostname() == true) {
            output.setHost(UvmContextFactory.context().networkManager().getFullyQualifiedHostname());
        } else {
            output.setHost(hostAddr.getHostAddress().toString());
        }

        // set the path of the capture handler
        output.setPath("/capture/handler.py/index");

        // set the scheme and port appropriately
        if (captureApp.getCaptivePortalSettings().getAlwaysUseSecureCapture() == true) {
            output.setScheme("https");
            if (httpsPort != 443) output.setPort(httpsPort);
        } else {
            output.setScheme("http");
            if (httpPort != 80) output.setPort(httpPort);
        }

        // add all off the parameters needed by the capture handler
        // VERY IMPORTANT - the NONCE value must be a1b2c3d4e5f6 because the
        // handler.py script looks for this special value and uses it to
        // decide between http and https when redirecting to the originally
        // requested page after login.  Yes it's a hack but I didn't want to
        // add an additional form field and risk breaking existing custom pages
        output.addParameter("nonce", "a1b2c3d4e5f6");
        output.addParameter("method", methodStr);
        output.addParameter("appid", appStr);
        output.addParameter("host", hostStr);
        output.addParameter("uri", uriStr);

        vector = "HTTP/1.1 307 Temporary Redirect\r\n";

        // if using Google authentication build the authentication redirect
        // and pass the output as the OAuth state, otherwise use directly
        if (captureApp.getCaptivePortalSettings().getAuthenticationType() == CaptivePortalSettings.AuthenticationType.GOOGLE) {
            exauth.setScheme("https");
            exauth.setHost(CaptivePortalReplacementGenerator.GOOGLE_AUTH_HOST);
            exauth.setPath(CaptivePortalReplacementGenerator.GOOGLE_AUTH_PATH);
            exauth.addParameter("client_id", CaptivePortalReplacementGenerator.GOOGLE_CLIENT_ID);
            exauth.addParameter("redirect_uri", CaptivePortalReplacementGenerator.AUTH_REDIRECT_URI);
            exauth.addParameter("response_type", "code");
            exauth.addParameter("scope", "email");
            exauth.addParameter("state", output.toString());
            vector += "Location: " + exauth.toString() + "\r\n";
        } else if (captureApp.getCaptivePortalSettings().getAuthenticationType() == CaptivePortalSettings.AuthenticationType.FACEBOOK) {
            exauth.setScheme("https");
            exauth.setHost(CaptivePortalReplacementGenerator.FACEBOOK_AUTH_HOST);
            exauth.setPath(CaptivePortalReplacementGenerator.FACEBOOK_AUTH_PATH);
            exauth.addParameter("client_id", CaptivePortalReplacementGenerator.FACEBOOK_CLIENT_ID);
            exauth.addParameter("redirect_uri", CaptivePortalReplacementGenerator.AUTH_REDIRECT_URI);
            exauth.addParameter("response_type", "code");
            exauth.addParameter("scope", "email");
            exauth.addParameter("state", output.toString());
            vector += "Location: " + exauth.toString() + "\r\n";
        } else {
            vector += "Location: " + output.toString() + "\r\n";
        }

        vector += "Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0\r\n";
        vector += "Pragma: no-cache\r\n";
        vector += "Expires: Mon, 10 Jan 2000 00:00:00 GMT\r\n";
        vector += "Content-Type: text/plain\r\n";
        vector += "Content-Length: 0\r\n";
        vector += "Connection: Close\r\n";
        vector += "\r\n";

        logger.debug("CLIENT REPLY = " + vector);

        // pass the reply buffer to the SSL engine wrap function
        ByteBuffer ibuff = ByteBuffer.wrap(vector.getBytes());
        ByteBuffer obuff = ByteBuffer.allocate(32768);
        result = sslEngine.wrap(ibuff, obuff);

        // we are done so we cleanup and release the session
        session.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_SSL_ENGINE, null);
        session.release();

        // return the now encrypted reply buffer back to the client
        ByteBuffer array[] = new ByteBuffer[1];
        obuff.flip();
        array[0] = obuff;
        session.sendDataToClient(array);
        return true;
    }

    public String extractSNIhostname(ByteBuffer data) throws Exception
    {
        int counter = 0;
        int pos;

        logger.debug("Searching for SNI in " + data.toString());

        // make sure we have a TLS handshake message
        int recordType = Math.abs(data.get());

        if (recordType != TLS_HANDSHAKE) {
            logger.debug("First byte is not TLS Handshake signature");
            return (null);
        }

        int sslVersion = data.getShort();
        int recLength = Math.abs(data.getShort());

        // make sure we have a ClientHello message
        int shakeType = Math.abs(data.get());

        if (shakeType != CLIENT_HELLO) {
            logger.debug("Handshake type is not ClientHello");
            return (null);
        }

        // extract all the handshake data so we can get to the extensions
        int messHilen = data.get();
        int messLolen = data.getShort();
        int clientVersion = data.getShort();
        int clientTime = data.getInt();

        // skip over the fixed size client random data 
        if (data.remaining() < 28) throw new BufferUnderflowException();
        pos = data.position();
        data.position(pos + 28);

        // skip over the variable size session id data
        int sessionLength = Math.abs(data.get());
        if (sessionLength > 0) {
            if (data.remaining() < sessionLength) throw new BufferUnderflowException();
            pos = data.position();
            data.position(pos + sessionLength);
        }

        // skip over the variable size cipher suites data
        int cipherLength = Math.abs(data.getShort());
        if (cipherLength > 0) {
            if (data.remaining() < cipherLength) throw new BufferUnderflowException();
            pos = data.position();
            data.position(pos + cipherLength);
        }

        // skip over the variable size compression methods data
        int compLength = Math.abs(data.get());
        if (compLength > 0) {
            if (data.remaining() < compLength) throw new BufferUnderflowException();
            pos = data.position();
            data.position(pos + compLength);
        }

        // if the position equals recLength plus 5 we know this is the end
        // of the packet and thus there are no extensions - will normally
        // be equal but we include the greater than just to be safe
        if (data.position() >= (recLength + 5)) {
            logger.debug("No extensions found in TLS handshake message");
            return (null);
        }

        // get the total size of extension data block
        int extensionLength = Math.abs(data.getShort());

        while (counter < extensionLength) {
            int extType = Math.abs(data.getShort());
            int extSize = Math.abs(data.getShort());

            // if not server name extension adjust the offset to the next
            // extension record and continue
            if (extType != SERVER_NAME) {
                data.position(data.position() + extSize);
                counter += (extSize + 4);
                continue;
            }

            // we read the name list info by passing the offset location so we
            // don't modify the position which makes it easier to skip over the
            // whole extension if we bail out during name extraction
            int listLength = Math.abs(data.getShort(data.position()));
            int nameType = Math.abs(data.get(data.position() + 2));
            int nameLength = Math.abs(data.getShort(data.position() + 3));

            // if we find a name type we don't understand we just abandon
            // processing the rest of the extension
            if (nameType != HOST_NAME) {
                data.position(data.position() + extSize);
                counter += (extSize + 4);
                continue;
            }

            // found a valid host name so adjust the position to skip over
            // the list length and name type info we directly accessed above
            data.position(data.position() + 5);
            byte[] hostData = new byte[nameLength];
            data.get(hostData, 0, nameLength);
            String hostName = new String(hostData);
            logger.debug("Extracted SNI hostname = " + hostName);
            return hostName.toLowerCase();
        }

        return (null);
    }

    private TrustManager trust_all_certificates = new X509TrustManager()
    {
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }
    };
}
