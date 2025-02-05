/**
 * $Id: WebFilterHttpsSniHandler.java 42225 2016-01-24 01:25:31Z dmorris $
 */

package com.untangle.app.threat_prevention;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.BufferUnderflowException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import com.untangle.app.http.HttpMethod;
import com.untangle.app.http.RequestLine;
import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.TlsHandshakeException;
import com.untangle.app.http.HttpRedirect;
import com.untangle.app.http.HttpRequestEvent;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.untangle.app.http.HttpUtility;

/**
 * Extracts the SNI information from HTTPS ClientHello messages and does block
 * and pass checking
 * 
 * @author mahotz
 * 
 */
public class ThreatPreventionHttpsSniHandler extends AbstractEventHandler
{
    private final Logger logger = LogManager.getLogger(getClass());
    private ThreatPreventionApp app;

    // these are used while extracting the SNI from the SSL ClientHello packet
    private static int TLS_HANDSHAKE = 0x16;
    private static int CLIENT_HELLO = 0x01;
    private static int SERVER_NAME = 0x0000;
    private static int HOST_NAME = 0x00;

    /**
     * Constructor
     * 
     * @param app
     *        The web filter base application
     */
    public ThreatPreventionHttpsSniHandler(ThreatPreventionApp app)
    {
        super(app);

        this.app = app;
        logger.debug("Created ThreatPreventionHttpSniHandler");
    }

    /**
     * Handle new session requests
     * 
     * @param req
     *        The session request
     */
    public void handleTCPNewSessionRequest(TCPNewSessionRequest req)
    {
        if (req.getNewServerPort() != 443) {
            req.release();
            return;
        }
    }

    /**
     * Handles session chunks
     * 
     * @param session
     *        The session
     * @param data
     *        The data chunk
     */
    public void handleTCPClientChunk(AppTCPSession session, ByteBuffer data)
    {
        // grab the SSL Inspector status attachment
        Boolean sslInspectorStatus = (Boolean) session.globalAttachment(AppSession.KEY_SSL_INSPECTOR_SESSION_INSPECT);

	// if the license is not valid or
        // if we find the attachment and it is true then we release the
        // session now since we'll see the unencrypted traffic later
        if (app.isLicenseValid() != true || ((sslInspectorStatus != null) && (sslInspectorStatus.booleanValue() == true))) {
            session.sendDataToServer(data);
            session.release();
            return;
        }

        // see if there is an SSL engine attached to the session
        ThreatPreventionSSLEngine engine = (ThreatPreventionSSLEngine) session.globalAttachment(AppSession.KEY_THREAT_PREVENTION_SSL_ENGINE);

        if (engine != null) {
            // found an engine which means we've decided to block so we pass
            // all received data to the SSL engine which will create and
            // encrypt the redirect and return it for transmit to the client
            engine.handleClientData(data);
            return;
        } else {
            // no engine attached so we're still analyzing this thing
            checkClientRequest(session, data);
            return;
        }
    }

    /**
     * Checks client requests
     * 
     * @param sess
     *        The session
     * @param data
     *        The data
     */
    private void checkClientRequest(AppTCPSession sess, ByteBuffer data)
    {
        java.security.cert.X509Certificate serverCert = null;
        ByteBuffer buff = data;
        LdapName ldapName = null;
        String domain = null;

        // grab any buffer that might have been attached last time thru here
        ByteBuffer hold = (ByteBuffer) sess.attachment();

        // if the session has a buffer attached then we got an underflow
        // exception while handling the last chunk so we grab and clear
        if (hold != null) {
            sess.attach(null);

            // if no room in the hold buffer then just give up
            if ((hold.position() + buff.limit()) > hold.capacity()) {
                logger.debug("Giving up after {} bytes", hold.position());
                sess.release();
                ByteBuffer array[] = new ByteBuffer[1];
                array[0] = hold;
                sess.sendDataToServer(array);
                return;
            }

            hold.put(buff); // append the new data to the hold buffer
            buff = hold; // make working buffer point to the hold buffer
            buff.flip(); // flip the working buffer
        }

        logger.debug("HANDLE_CHUNK = {}", buff.toString());

        // scan the buffer for the SNI hostname
        try {
            domain = HttpUtility.extractSniHostname(buff.duplicate());
        }

        // on underflow exception we stuff the partial packet into a buffer
        // and attach to the session then return to wait for more data
        catch (BufferUnderflowException exn) {
            logger.debug("Buffer underflow... waiting for more data");
            logger.debug("Buffer underflow... waiting for more data");
            hold = ByteBuffer.allocate(4096);
            hold.put(buff);
            sess.attach(hold);
            return;
        }

        // For any handshake exception we just log
        catch (TlsHandshakeException exn) {
            logger.warn("Exception while handling packet : {}", exn.getMessage());
        }

        // any other exception we just log, release, and return
        catch (Exception exn) {
            logger.warn("Exception calling extractSNIhostname ", exn);
            sess.release();
            ByteBuffer array[] = new ByteBuffer[1];
            array[0] = buff;
            sess.sendDataToServer(array);
            return;
        }

        if (domain != null) logger.debug("Detected SSL connection (via SNI) to: {} ", domain);

        /**
         * If SNI information is not present then we fallback to using the
         * certificate CN if the option is enabled
         */
        // if ((domain == null) && (app.isHttpsEnabledSniCertFallback())) {
        if (domain == null) {

            // grab the cached certificate for the server
            serverCert = UvmContextFactory.context().certCacheManager().fetchServerCertificate(sess.getServerAddr().getHostAddress().toString());

            // if we found the server certificate extract the host name
            if (serverCert != null) {

                try {
                    // grab the subject distinguished name from the certificate
                    ldapName = new LdapName(serverCert.getSubjectX500Principal().getName());
                }

                catch (Exception exn) {
                    logger.warn("Exception extracting host name from server certificate ", exn);
                }

                if(ldapName != null){
                    // we only want the CN from the certificate
                    for (Rdn rdn : ldapName.getRdns()) {
                        if (rdn.getType().equals("CN") == false) continue;
                        domain = rdn.getValue().toString().toLowerCase();
                        break;
                    }
                }
            }

            if (domain != null) {
                logger.debug("Detected SSL connection (via CERT) to: {}", domain);
            }
        }

        /**
         * If we didn't get a hostname from SNI or the certificate CN then we
         * revert to IP-based if its enabled
         */
        if (domain == null) {
            domain = sess.getServerAddr().getHostAddress();
        }

        if (domain == null) {
            logger.debug("No SNI information was found.");
            sess.release();
            ByteBuffer array[] = new ByteBuffer[1];
            array[0] = buff;
            sess.sendDataToServer(array);
            return;
        }

        /*
         * Since we process traffic long before the http-casing gets the
         * traffic, lots of http related stuff isn't available, so we use a
         * dummy RequestLine to log our events. We don't want multiple apps
         * parsing SNI to create duplicate event entries, so we check to see if
         * there is an existing RequestLine attached. If so we use it otherwise
         * we create and attach it ourselves.
         */
        RequestLine requestLine = null;

        requestLine = (RequestLine) sess.globalAttachment(AppSession.KEY_HTTPS_SNI_REQUEST_LINE);
        if (requestLine == null) {
            requestLine = new RequestLine(sess.sessionEvent(), HttpMethod.GET, new byte[] { '/' });
            sess.globalAttach(AppSession.KEY_HTTPS_SNI_REQUEST_LINE, requestLine);
            logger.debug("Creating new requestLine: {}", requestLine.toString());
        } else {
            logger.debug("Using existing requestLine: {}", requestLine.toString());
        }

        String encodedDomain = URLEncoder.encode(domain, StandardCharsets.UTF_8);
        URI fakeUri;
        try {
            fakeUri = new URI("/");
            /**
             * Test that https://domain/ is a valid URL
             */
            URI uri = new URI("https://" + encodedDomain + "/");
        } catch (Exception e) {
            if (e.getMessage().contains("Illegal character")) {
                logger.error("Could not parse (illegal character): {}", domain);
            } else {
                logger.error("Could not parse URI for {}",  domain, e);
            }

            /**
             * If we couldn't parse the URL/hostname there isn't much we can do
             * at this point
             */
            sess.release();
            ByteBuffer array[] = new ByteBuffer[1];
            array[0] = buff;
            sess.sendDataToServer(array);
            return;
        }
        requestLine.setRequestUri(fakeUri); // URI is unknown

        /**
         * Similar to the RequestLine logic above, this session may already have a requestlinetoken associated with it,
         * we do not want to add multiple requestlinetokens for each individual app.
         * 
         * This logic should be consolidated/removed.
         */

        RequestLineToken rlt = null;

        rlt = (RequestLineToken) sess.globalAttachment(AppSession.KEY_HTTPS_SNI_REQUEST_TOKEN);
        if (rlt == null) {
            rlt = new RequestLineToken(requestLine, "HTTP/1.1");
            sess.globalAttach(AppSession.KEY_HTTPS_SNI_REQUEST_TOKEN, rlt);
            logger.debug("Creating new requestLineToken: {}", rlt.toString());
        } else {
            logger.debug("Using existing requestLine: {}", rlt.toString());
        }

        /**
         * Log this HTTPS hit to the http_events table
         * 
         * 
         * Similar to the RequestLine and RequestLineToken logic above, 
         * we do not want to create an additional HttpRequestEvent in the http_events
         * table when a request comes through the SNI logic.
         * 
         * This logic should be consolidated/removed.
         * 
         */
        HttpRequestEvent evt = null;
        
        evt = (HttpRequestEvent) sess.globalAttachment(AppSession.KEY_HTTPS_SNI_HTTP_REQUEST_EVENT);

        if (evt == null) {
            evt = new HttpRequestEvent(requestLine, encodedDomain, null, 0);
            requestLine.setHttpRequestEvent(evt);
            this.app.logEvent(evt);
            sess.globalAttach(AppSession.KEY_HTTPS_SNI_HTTP_REQUEST_EVENT, evt);
            logger.debug("Creating new HttpRequestEvent: {}", evt.toString());
        } else {
            logger.debug("Using existing HttpRequestEvent:{} ", evt.toString());
        }

        // attach the hostname we extracted to the session
        sess.globalAttach(AppSession.KEY_HTTP_HOSTNAME, encodedDomain);

        HeaderToken h = new HeaderToken();
        h.addField("host", encodedDomain);

        // pass the info to the decision engine to see if we should block
        HttpRedirect redirect = app.getDecisionEngine().checkRequest(sess, sess.getClientAddr(), 443, rlt, h);

        // we have decided to block so we create the SSL engine and start
        // by passing it all the client data received thus far
        if (redirect != null) {
            logger.debug(" ----------------BLOCKED: {} traffic----------------", domain);
            logger.debug("TCP: {}:{} -> {}:{}", 
            sess.getClientAddr().getHostAddress(), 
            sess.getClientPort(), 
            sess.getServerAddr().getHostAddress(), 
            sess.getServerPort());

            ThreatPreventionSSLEngine engine;
            if(app.getSettings().getCloseHttpsBlockEnabled()){
                sess.killSession();
            }else{
                engine = new ThreatPreventionSSLEngine(sess, redirect.getResponse());
                sess.globalAttach(AppSession.KEY_THREAT_PREVENTION_SSL_ENGINE, engine);
                engine.handleClientData(buff);
            }
            return;
        }

        // We release session immediately upon first match.
        sess.release();
        ByteBuffer array[] = new ByteBuffer[1];
        array[0] = buff;
        sess.sendDataToServer(array);
        return;
    }
}
