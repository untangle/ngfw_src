/**
 * $Id: WebFilterHttpsSniHandler.java 42225 2016-01-24 01:25:31Z dmorris $
 */

package com.untangle.app.threat_prevention;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.BufferUnderflowException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import com.untangle.app.http.HttpMethod;
import com.untangle.app.http.RequestLine;
import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.HttpRequestEvent;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import org.apache.log4j.Logger;

/**
 * Extracts the SNI information from HTTPS ClientHello messages and does block
 * and pass checking
 * 
 * @author mahotz
 * 
 */
public class ThreatPreventionHttpsSniHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
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

        // if we find the attachment and it is true then we release the
        // session now since we'll see the unencrypted traffic later
        if ((sslInspectorStatus != null) && (sslInspectorStatus.booleanValue() == true)) {
            session.sendDataToServer(data);
            session.release();
            return;
        }

        // see if there is an SSL engine attached to the session
        ThreatPreventionSSLEngine engine = (ThreatPreventionSSLEngine) session.globalAttachment(AppSession.KEY_WEB_FILTER_SSL_ENGINE);
        // ThreatPreventionSSLEngine engine = null;

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
                logger.debug("Giving up after " + hold.position() + " bytes");
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

        logger.debug("HANDLE_CHUNK = " + buff.toString());
        // app.incrementScanCount();

        // scan the buffer for the SNI hostname
        try {
            domain = extractSNIhostname(buff.duplicate());
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

        // any other exception we just log, release, and return
        catch (Exception exn) {
            logger.warn("Exception calling extractSNIhostname ", exn);
            sess.release();
            ByteBuffer array[] = new ByteBuffer[1];
            array[0] = buff;
            sess.sendDataToServer(array);
            return;
        }

        if (domain != null) logger.debug("Detected SSL connection (via SNI) to: " + domain);

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
                    ldapName = new LdapName(serverCert.getSubjectDN().getName());
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
                logger.debug("Detected SSL connection (via CERT) to: " + domain);
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

        RequestLine requestLine = new RequestLine(sess.sessionEvent(), HttpMethod.GET, new byte[] { '/' });

        URI fakeUri;
        try {
            fakeUri = new URI("/");
            /**
             * Test that https://domain/ is a valid URL
             */
            URI uri = new URI("https://" + domain + "/");
        } catch (Exception e) {
            if (e.getMessage().contains("Illegal character")) {
                logger.error("Could not parse (illegal character): " + domain);
            } else {
                logger.error("Could not parse URI for " + domain, e);
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
        RequestLineToken rlt = new RequestLineToken(requestLine, "HTTP/1.1");

        /**
         * Log this HTTPS hit to the http_events table
         */
        HttpRequestEvent evt = new HttpRequestEvent(requestLine, domain, null, 0);
        requestLine.setHttpRequestEvent(evt);
        this.app.logEvent(evt);

        // attach the hostname we extracted to the session
        sess.globalAttach(AppSession.KEY_HTTP_HOSTNAME, domain);

        HeaderToken h = new HeaderToken();
        h.addField("host", domain);

        // pass the info to the decision engine to see if we should block
        ThreatPreventionBlockDetails redirectDetails = app.getDecisionEngine().checkRequest(sess, sess.getClientAddr(), 443, rlt, h);

        // we have decided to block so we create the SSL engine and start
        // by passing it all the client data received thus far
        if (redirectDetails != null) {
            app.incrementBlockCount();
            logger.debug(" ----------------BLOCKED: " + domain + " traffic----------------");
            logger.debug("TCP: " + sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " + sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort());

            Token[] response = null;
            response = app.generateHttpResponse( redirectDetails, sess);
            ThreatPreventionSSLEngine engine = new ThreatPreventionSSLEngine(sess, response);
            sess.globalAttach(AppSession.KEY_WEB_FILTER_SSL_ENGINE, engine);
            engine.handleClientData(buff);
            return;
        }

        // We release session immediately upon first match.
        sess.release();
        ByteBuffer array[] = new ByteBuffer[1];
        array[0] = buff;
        sess.sendDataToServer(array);
        return;
    }

    /**
     * Extract the SNI hostname from a ClientHello message. We don't bother
     * checking the buffer position or length on much of the stuff here since
     * the caller uses the buffer underflow exception to know when it needs to
     * wait for more data when a full packet has not yet been received.
     * 
     * @param data
     * @return The SNI hostname if found, otherwise null
     * @throws Exception
     */
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
}
