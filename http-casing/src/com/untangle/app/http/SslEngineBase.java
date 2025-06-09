/**
 * $Id: $
 */
package com.untangle.app.http;

import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.KeyManagerFactory;

import java.nio.ByteBuffer;
import java.io.FileInputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyStore;

import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.UvmContextFactory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.vnet.Token;
import com.untangle.app.http.HeaderToken;
import com.untangle.app.http.StatusLine;

/**
 * This class is base for all the ssl redirection that we do in webfilter,
 * threat prevention and captive portal before ssl redirections we have done
* just enough processing of HTTPS sessions using SSLEngine so we can
* return a block page when required. It will always cause a browser warning,
* either because our cert isn't trusted or because we're redirecting to a
* different place than the client requested, but we figure in most cases it's
* better than just dropping the session and letting the client timeout.
* 
* @author mahotz
* 
*/
public class SslEngineBase
{
    private final Logger logger = LogManager.getLogger(getClass());
    private SSLContext sslContext;
    private SSLEngine sslEngine;
    private Token[] response;

    /**
     * Default Constructor
     * Initilise the keystore and SSLEngine
    */
    public SslEngineBase()
    {
        String webCertFile = CertificateManager.CERT_STORE_PATH + UvmContextFactory.context().systemManager().getSettings().getWebCertificate().replaceAll("\\.pem", "\\.pfx");
        try {
            // use the argumented certfile and password to init our keystore
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(webCertFile)) {
                keyStore.load(fis, CertificateManager.CERT_FILE_PASSWORD.toCharArray());
            }
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
            logger.error("Exception creating SslEngineBase()", exn);
        }
    }

    /**
    * Constructor
    * 
    * @param response
    *        Token[] of https response to send.
    */
    public SslEngineBase(Token[] response)
    {
        this();
        this.response = response;
    }

    /**
     * Handles a chunk of client data
    * 
    * @param session
    *        The session
    * @param buff
    *        The client data
     @param key
    *        App specific ssl key
    */
    public void handleClientData(AppTCPSession session, ByteBuffer buff, String key)
    {
        // pass the data to the client data worker function
        boolean success = false;

        try {
            success = clientDataWorker(session, buff, key);
        }

        // catch any exceptions
        catch (Exception exn) {
            logger.debug("Exception calling clientDataWorker", exn);
        }

        // null result means something went haywire
        if (!success) {
            session.globalAttach(key, null);
            session.resetClient();
            session.resetServer();
            session.release();
        }

        return;
    }

    /**
     * Processes a chunk of client data
    * @param session
    *        The session
    * @param data
    *        The data
    * @return True if processing was successful, otherwise false
    * @param key
    *        App specific ssl key
    * @throws Exception
    */
    public boolean clientDataWorker(AppTCPSession session, ByteBuffer data, String key) throws Exception
    {
        boolean done = false;
        HandshakeStatus status;
        logger.debug("PARAM_BUFFER = {}", data.toString());
        boolean isClientDataProcessed = clientDataProcessor(session, data);
        if(isClientDataProcessed) return true;
        while (!done) {
            status = sslEngine.getHandshakeStatus();
            logger.debug("STATUS = {}", status);

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
                done = doNeedTask();
                break;

            // handle unwrap during handshake
            case NEED_UNWRAP:
                done = doNeedUnwrap(session, data);
                break;

            // handle wrap during handshake
            case NEED_WRAP:
                done = doNeedWrap(session, data);
                break;

            // handle data when no handshake is in progress
            case NOT_HANDSHAKING:
                done = doNotHandshaking(session, data, key);
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
     * Called when SSLEngine status = NEED_TASK. We call run for all outstanding
    * tasks and then return false to break out of the parser processing loop so
    * we can receive more data.
    * 
    * @return False
    * @throws Exception
    */
    private boolean doNeedTask() throws Exception
    {
        Runnable runnable;

        // loop and run SSLEngine outstanding tasks
        while ((runnable = sslEngine.getDelegatedTask()) != null) {
            logger.debug("EXEC_TASK {}", runnable.toString());
            runnable.run();
        }
        return false;
    }

    /**
     * Called when SSLEngine status = NEED_UNWRAP
    * @param session
    *        TCP session
    * @param data
    *        The data received
    * @return True to continue the parser loop, false to break out
    * @throws Exception
    */
    private boolean doNeedUnwrap(AppTCPSession session, ByteBuffer data) throws Exception
    {
        SSLEngineResult result;
        ByteBuffer target = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize() + 50);

        // unwrap the argumented data into the engine buffer
        result = sslEngine.unwrap(data, target);
        logger.debug("EXEC_UNWRAP {}" , result.toString());

        boolean verifySSLstatus = verifySSLstatus(result, data);

        if(verifySSLstatus) return true;

        if (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
            // underflow during unwrap means the SSLEngine needs more data
            // but it's also possible it used some of the passed data so we
            // compact the receive buffer and hand it back for more
            data.compact();
            logger.debug("UNDERFLOW_LEFTOVER = {}", data.toString());

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
    * @param session
    *        TCP session
    * @param data
    *        The data received
    * @return True to continue the parser loop, false to break out
    * @throws Exception
    */
    private boolean doNeedWrap(AppTCPSession session, ByteBuffer data) throws Exception
    {
        SSLEngineResult result;
        ByteBuffer target = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());

        // wrap the argumented data into the engine buffer
        result = sslEngine.wrap(data, target);
        logger.debug("EXEC_WRAP {}", result.toString());

        // check for engine problems
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine wrap fault");

        // If we produced something for the other side, send it.
        if (result.bytesProduced() != 0){
            target.flip();
            session.sendDataToClient(target);
        }

        // if the engine result hasn't changed we need more processing
        if (result.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) return false;

        // More data here almost certainly means we've transitioned to FINISHED/NOT_HANDSHAKING mode
        // and the remainder should be handled there.
        if(data.remaining() > 0 ) return false;

        return true;
    }

    /**
     * Called when we receive data and dataMode is true, meaning we're done with
    * the handshake and we're now passing data back and forth between the two
    * sides.
    * @param session
    *        The session
    * @param data
    *        The data received
    * @return True to continue the parser loop, false to break out
    * @param key
    *        App specific ssl key
    * @throws Exception
    */
    private boolean doNotHandshaking(AppTCPSession session, ByteBuffer data, String key) throws Exception
    {
        SSLEngineResult result = null;
        String vector = new String();
        ByteBuffer target = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());

        // we call unwrap for all data we receive from the client 
        result = sslEngine.unwrap(data, target);
        logger.debug("EXEC_HANDSHAKING {}", result.toString());
        logger.debug("LOCAL_BUFFER = {}", target.toString());

        // make sure we get a good status return from the SSL engine
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine unwrap fault");

        // if unwrap doesn't produce any data then we are handshaking and 
        // must return null to let the handshake process continue
        if (result.bytesProduced() == 0) return false;

        // when unwrap finally returns some data it will be the client request
        String request = new String(target.array(), 0, target.position());
        logger.debug("CLIENT REQUEST = {}", request);
        Token[] generatedResponse = generateResponse(request, session);
        if(generatedResponse == null) return true;
        for(Token token : generatedResponse){
            if( token instanceof StatusLine ){
                vector += ((StatusLine) token).getString();
            }else if( token instanceof HeaderToken){
                vector += ((HeaderToken) token).getString();
            }
        }

        logger.debug("CLIENT REPLY = {}", vector);

        // pass the reply buffer to the SSL engine wrap function
        ByteBuffer ibuff = ByteBuffer.wrap(vector.getBytes());
        ByteBuffer obuff = ByteBuffer.allocate(32768);
        result = sslEngine.wrap(ibuff, obuff);

        // we are done so cleanup attachment and release session
        session.globalAttach(key, null);
        session.release();

        // return the now encrypted reply buffer back to the client
        obuff.flip();
        session.sendDataToClient(obuff);

        return true;
    }
    /**
     * In case of webfilter and threatprevention no addition process is required hence returning false
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The raw data from client
     * @return True if processing was successful, otherwise false
     */
    public boolean clientDataProcessor(AppTCPSession session, ByteBuffer data){
        return false;
    }

    /**
     * Get the response for webfilter and threatprevention
     * @param request
     *        Client request
     * @param session
     *        The TCP session
     * @return response if request parse successfully else null
     */
    public Token[] generateResponse(String request, AppTCPSession session){
        return this.response;
    }

    /**
     * verify ssl status for webfilter and threatprevention 
     * @param result
     *        SSL engine result
     * @param data
     *        The raw data from client
     * @return true if status is ok else false
     */
    public boolean verifySSLstatus(SSLEngineResult result,  ByteBuffer data){
        // Nothing more to process.
        if(result.getStatus() == SSLEngineResult.Status.OK && !data.hasRemaining()) return true;
        return false;
    }

    /**
     * This is a dumb trust manager that will blindly trust all server
    * certficiates. We use it here simply to prevent SSLEngine from loading the
    * cacerts since we don't ever interact with the server.
    */
    private TrustManager trust_all_certificates = new X509TrustManager()
    {
        /**
         * Throw nothing, trust everything
        * 
        * @param chain
        * @param authType
        * @throws CertificateException
        */
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        /**
         * Throw nothing, trust everything
        * 
        * @param chain
        * @param authType
        * @throws CertificateException
        */
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
        {
        }

        /**
         * @return null to accept all issuers
        */
        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }
    };
}

