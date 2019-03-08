/**
 * $Id$
 */
package com.untangle.app.web_cache;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.ByteBuffer;
import java.io.IOException;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.AppTCPSession;
import org.apache.log4j.Logger;

/**
 * This is the main network event handler
 * 
 * @author mahotz
 * 
 */
public class WebCacheStreamHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private WebCacheApp app;

    /**
     * Constructor
     * 
     * @param app
     *        The application that created us
     */
    public WebCacheStreamHandler(WebCacheApp app)
    {
        super(app);
        this.app = app;
        logger.debug("WEBCACHE WebCacheStreamHandler()");
    }

    /**
     * Handler for new client requests
     * 
     * @param session
     *        The TCP session
     */
    @Override
    public void handleTCPNewSession(AppTCPSession session)
    {
        logger.debug("WEBCACHE handleTCPNewSession()");

        if (app.isLicenseValid() != true) {
            this.app.incrementMetric(WebCacheApp.STAT_SYSTEM_BYPASS);
            app.statistics.IncSystemCount();
            session.release();
            return;
        }

        // if high load bypass flag is true release immediately
        if (app.highLoadBypass == true) {
            logger.debug("----- RELEASING SESSION DUE TO HIGH LOAD -----\n");
            this.app.incrementMetric(WebCacheApp.STAT_SYSTEM_BYPASS);
            app.statistics.IncSystemCount();
            session.release();
            return;
        }

        // allocate a new object for session info and init key members
        WebCacheSessionInfo sessInfo = new WebCacheSessionInfo();
        sessInfo.clientBuffer = ByteBuffer.allocate(app.CLIENT_BUFFSIZE);
        sessInfo.myIndex = WebCacheParent.INSTANCE.GetCacheIndex();
        session.attach(sessInfo);
        super.handleTCPNewSession(session);
    }

    /**
     * Handler for finalized sessions
     * 
     * @param session
     */
    @Override
    public void handleTCPFinalized(AppTCPSession session)
    {
        WebCacheSessionInfo sessInfo = (WebCacheSessionInfo) session.attachment();
        logger.debug("WEBCACHE handleTCPFinalized()");

        if (sessInfo != null) {
            ParentCleanup(sessInfo);
            SquidCleanup(sessInfo);
        }

        super.handleTCPFinalized(session);
    }

    /**
     * Handler for client data
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     */
    @Override
    public void handleTCPClientChunk(AppTCPSession session, ByteBuffer data)
    {
        ByteBuffer chunk = data;
        logger.debug("WEBCACHE handleTCPClientChunk received " + chunk.limit() + " bytes");

        // pass the event to our client request function
        processClientRequest(session, data);
        return;
    }

    /**
     * Handler for client data end
     * 
     * @param session
     *        The TCP session
     * 
     * @param data
     *        The data received
     */
    @Override
    public void handleTCPClientDataEnd(AppTCPSession session, ByteBuffer data)
    {
        WebCacheSessionInfo sessInfo = (WebCacheSessionInfo) session.attachment();
        logger.debug("WEBCACHE handleTCPClientDataEnd");
        ParentCleanup(sessInfo);
        SquidCleanup(sessInfo);
        super.handleTCPClientDataEnd(session, data);
    }

    /**
     * Handler for server data
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     */
    @Override
    public void handleTCPServerChunk(AppTCPSession session, ByteBuffer data)
    {
        if (app.SOCKET_DEBUG == true) logger.debug("WEBCACHE handleTCPServerChunk received " + data.limit() + " bytes");

        // pass the event to our server response function
        processServerResponse(session, data);

        return;
    }

    /**
     * Handler for server data end
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     */
    @Override
    public void handleTCPServerDataEnd(AppTCPSession session, ByteBuffer data)
    {
        WebCacheSessionInfo sessInfo = (WebCacheSessionInfo) session.attachment();
        logger.debug("WEBCACHE handleTCPServerDataEnd");
        ParentCleanup(sessInfo);
        SquidCleanup(sessInfo);
        super.handleTCPServerDataEnd(session, data);
    }

    /**
     * Handler for client requests. As of HTTP 1.1 the following methods are
     * supported: OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT. We
     * process those that make sense for us, and release the others.
     * 
     * @param sess
     *        The TCP session
     * @param data
     *        The data received
     */
    private void processClientRequest(AppTCPSession sess, ByteBuffer data)
    {
        WebCacheSessionInfo sessInfo = (WebCacheSessionInfo) sess.attachment();
        ByteBuffer chunk = data;
        ByteBuffer[] buff = new ByteBuffer[1];
        String hostname = null;
        int cache = 0, retval = 0;
        int want, have, need, tail, top, end;

        // if new data would overflow client buffer we allocate a new buffer
        // and stuff everything we have thus far into it and then release
        // the session returning all the data we have to the server
        if (sessInfo.clientBuffer.remaining() < chunk.limit()) {
            logger.debug("----- RELEASING OVERFLOW SESSION -----");
            ParentCleanup(sessInfo);
            SquidCleanup(sessInfo);
            sess.attach(null);
            sess.release();
            this.app.incrementMetric(WebCacheApp.STAT_SYSTEM_BYPASS);
            app.statistics.IncSystemCount();

            sessInfo.clientBuffer.flip();
            buff[0] = ByteBuffer.allocate(sessInfo.clientBuffer.limit() + chunk.limit());
            buff[0].put(sessInfo.clientBuffer);
            buff[0].put(chunk);
            buff[0].flip();
            sess.sendDataToServer(buff);
            return;
        }

        // add the received data to our session buffer
        sessInfo.clientBuffer.put(chunk);
        chunk.rewind();

        // convert the client buffer to a string we can scan
        String orgstr = new String(sessInfo.clientBuffer.array(), 0, sessInfo.clientBuffer.position());

        // look for any request methods that we support
        if (orgstr.toUpperCase().startsWith("POST") == true) cache++;
        if (orgstr.toUpperCase().startsWith("HEAD") == true) cache++;
        if (orgstr.toUpperCase().startsWith("GET") == true) cache++;

        // release the session if we do not support the request method
        if (cache == 0) {
            logger.debug("----- RELEASING UNSUPPORTED METHOD SESSION -----\n");
            ParentCleanup(sessInfo);
            SquidCleanup(sessInfo);
            sess.attach(null);
            sess.release();
            this.app.incrementMetric(WebCacheApp.STAT_SYSTEM_BYPASS);
            app.statistics.IncSystemCount();
            sess.sendDataToServer(data);
            return;
        }

        // see if we have a complete request
        tail = orgstr.indexOf("\r\n\r\n");

        // request not complete so no data is passed either way yet
        if (tail < 0) {
            return;
        }

        // calculate total size of request header
        have = (tail + 4);

        // need to figure out the content length for post requests
        if (orgstr.toUpperCase().startsWith("POST") == true) {
            String look = "CONTENT-LENGTH: ";
            top = orgstr.toUpperCase().indexOf(look);
            end = orgstr.indexOf("\r\n", top);

            // missing length so we send the full request directly to the server
            if ((top < 0) || (end < 0)) {
                logger.debug("----- CLIENT MISSING CONTENT LENGTH -----");
                sessInfo.clientBuffer.flip();
                buff[0] = sessInfo.clientBuffer;
                sessInfo.clientBuffer = ByteBuffer.allocate(app.CLIENT_BUFFSIZE);
                sess.sendDataToServer(buff);
                return;
            }

            // calculate total size of header and post content
            want = Integer.parseInt(orgstr.substring(top + look.length(), end));
            need = (have + want);

            // need more data from client so no data passed either way yet
            if (sessInfo.clientBuffer.position() < need) {
                logger.debug("----- CLIENT POST HEADER UNDERRUN (" + sessInfo.clientBuffer.position() + " of " + need + ") DETECTED -----");
                return;
            }

            // if we find part of the next request then the client is likely using
            // pipelining which we do not support so we release the session
            if (sessInfo.clientBuffer.position() > need) {
                logger.debug("----- RELEASING UNCACHEABLE POST SESSION -----\n" + orgstr.substring(0, tail));
                ParentCleanup(sessInfo);
                SquidCleanup(sessInfo);
                sess.attach(null);
                sess.release();
                this.app.incrementMetric(WebCacheApp.STAT_SYSTEM_BYPASS);
                app.statistics.IncSystemCount();
                sessInfo.clientBuffer.flip();
                buff[0] = sessInfo.clientBuffer;
                sess.sendDataToServer(buff);
                return;
            }

            // we have the full post request so we send the full request directly to the server
            logger.debug("----- CLIENT IGNORING " + need + " BYTE POST REQUEST -----\n" + orgstr.substring(0, tail));

            // cleanup any active squid parent socket channel
            ParentCleanup(sessInfo);

            sessInfo.clientBuffer.flip();
            buff[0] = sessInfo.clientBuffer;
            sessInfo.clientBuffer = ByteBuffer.allocate(app.CLIENT_BUFFSIZE);
            sess.sendDataToServer(buff);
            return;
        }

        // if we find part of the next request then the client is likely using
        // pipelining which we do not support so we release the session
        if (sessInfo.clientBuffer.position() > have) {
            logger.debug("----- RELEASING UNCACHEABLE PIPELINE SESSION -----\n");
            ParentCleanup(sessInfo);
            SquidCleanup(sessInfo);
            sess.attach(null);
            sess.release();
            this.app.incrementMetric(WebCacheApp.STAT_SYSTEM_BYPASS);
            app.statistics.IncSystemCount();
            sessInfo.clientBuffer.flip();
            buff[0] = sessInfo.clientBuffer;
            sess.sendDataToServer(buff);
            return;
        }

        // extract a lower case copy of the target host from the request
        String look = "HOST: ";
        top = orgstr.toUpperCase().indexOf(look);
        end = orgstr.indexOf("\r\n", top);
        if ((top >= 0) && (end >= 0)) hostname = new String(orgstr.substring(top + look.length(), end)).toLowerCase();

        // check the target host against the cache bypass list
        if (hostname != null) {
            boolean skip = false;
            int curr = 0;
            int next = 0;

            // walk down the hostname at each label and search
            // for a corresponding cache bypass entry
            for (;;) {
                logger.debug("CHECKING HOSTNAME: " + hostname.substring(next));

                if (app.settings.checkRules(hostname.substring(next)) == true) {
                    skip = true;
                    break;
                }

                next = hostname.indexOf('.', curr);
                if (next < curr) break;
                next++;
                curr = next;
            }

            if (skip == true) {
                logger.debug("----- CLIENT IGNORING USER BYPASS TARGET (" + hostname + ") -----");
                this.app.incrementMetric(WebCacheApp.STAT_USER_BYPASS);
                app.statistics.IncBypassCount();
                ParentCleanup(sessInfo);
                sessInfo.clientBuffer.flip();
                buff[0] = sessInfo.clientBuffer;
                sessInfo.clientBuffer = ByteBuffer.allocate(app.CLIENT_BUFFSIZE);
                sess.sendDataToServer(buff);
                return;
            }
        }

        // we have a full non-post request that is not in the bypass list so append
        // our session index and wrap in a new buffer that we can write to squid
        String modstr = new String(orgstr.substring(0, tail + 2));
        modstr += "X-Untangle-WebCache: " + sessInfo.myIndex + "\r\n\r\n";
        ByteBuffer modbb = ByteBuffer.wrap(modstr.getBytes(), 0, modstr.length());

        // cleanup any active squid parent socket channel
        ParentCleanup(sessInfo);

        logger.debug("----- CLIENT REQUEST (" + modstr.length() + " bytes) -----\n" + modstr);

        try {
            // add our session object to the app hashtable
            WebCacheParent.INSTANCE.InsertSockObject(sessInfo);

            // clean up any previous connection to squid
            if (sessInfo.squidChannel != null) SquidCleanup(sessInfo);

            // setup a new socket channel and connect to squid
            sessInfo.squidChannel = SocketChannel.open();

            // try to connect to squid running on localhost
            try {
                sessInfo.squidChannel.connect(new InetSocketAddress("127.0.0.1", 3128));
            }

            // connection failed so cleanup and release the session
            catch (IOException e) {
                ParentCleanup(sessInfo);
                SquidCleanup(sessInfo);
                sess.attach(null);
                sess.release();
                this.app.incrementMetric(WebCacheApp.STAT_SYSTEM_BYPASS);
                app.statistics.IncSystemCount();
                sessInfo.clientBuffer.flip();
                buff[0] = sessInfo.clientBuffer;
                sess.sendDataToServer(buff);
                return;
            }

            // switch the socket to non-blocking mode and create a selector
            // that the squid parent thread will use to signal us
            sessInfo.squidSelector = Selector.open();
            sessInfo.squidChannel.configureBlocking(false);
            sessInfo.squidKey = sessInfo.squidChannel.register(sessInfo.squidSelector, SelectionKey.OP_READ);

            // pass the inbound request to squid
            sessInfo.squidChannel.write(modbb);

            // wait for squid response or a wakeup from the cache parent thread
            retval = sessInfo.squidSelector.select(app.SELECT_TIMEOUT);

            // cleanup the socket selector but stay in non-blocking mode to make sure
            // we don't ever hang reading the client side data back from squid
            sessInfo.squidKey.cancel();
            sessInfo.squidKey = null;
            sessInfo.squidSelector.close();
            sessInfo.squidSelector = null;

            // remove our session object from the app hashtable
            WebCacheParent.INSTANCE.RemoveSockObject(sessInfo);

            // select returned zero which means squid did NOT give us a response and we were
            // awakend from the cache parent thread so send the full request to the server
            if (retval == 0) {
                logger.debug("===== CACHE MISS DETECTED =====");
                this.app.incrementMetric(WebCacheApp.STAT_MISS);
                app.statistics.IncMissCount();
                sessInfo.clientBuffer.flip();
                buff[0] = sessInfo.clientBuffer;
                sessInfo.clientBuffer = ByteBuffer.allocate(app.CLIENT_BUFFSIZE);
                sess.sendDataToServer(buff);
                return;
            }

            // squid has the object in cache so nothing will be sent to the
            // server and we will return the response from cache
            logger.debug("===== CACHE HIT DETECTED =====");
            this.app.incrementMetric(WebCacheApp.STAT_HIT);
            app.statistics.IncHitCount();

            // clear the client buffer for the next request
            sessInfo.clientBuffer.clear();

            // switch back to blocking mode
            sessInfo.squidChannel.configureBlocking(true);

            // stream the cached squid content back to the client
            sess.sendStreamerToClient(new WebCacheStreamer(app, sessInfo));
        }

        catch (Exception exn) {
            logger.error("Exception handling client request", exn);
        }

        // nothing gets sent to the original target server and the streamer
        // will handle sending the squid content back to the client
        return;
    }

    /**
     * Handler for server responses. We always send the response to the client,
     * and we also push it to squid if it should be cached.
     * 
     * @param sess
     *        The TCP session
     * @param data
     *        The data received
     */
    private void processServerResponse(AppTCPSession sess, ByteBuffer data)
    {
        ByteBuffer chunk = data;
        WebCacheSessionInfo sessInfo = (WebCacheSessionInfo) sess.attachment();
        ByteBuffer rxbuff = ByteBuffer.allocate(sess.serverReadBufferSize());

        try {
            // if squid has connected to us as a parent cache we mirror
            // everything we receive to the open socket channel
            if (sessInfo.parentChannel != null) {
                sessInfo.parentChannel.write(chunk);
                chunk.rewind();
            }

            // we also have to read the data back from the connection
            // we opened to squid otherwise it will hang
            if (sessInfo.squidChannel != null) {
                sessInfo.squidChannel.read(rxbuff);
                rxbuff.clear();
            }
        }

        // we see broken pipe errors when the client uses http pipelining that we didn't detect
        // while processing the request - squid closes the cache peer socket when it gets
        // unexpected data from the subsequent request so we just quietly cleanup the socket
        catch (IOException e) {
            ParentCleanup(sessInfo);
        }

        catch (Exception exn) {
            logger.error("Exception handling server response", exn);
            ParentCleanup(sessInfo);
        }

        app.statistics.AddMissBytes(chunk.remaining());
        sess.sendDataToClient(data);
        return;
    }

    /**
     * Called to close and cleanup the parent channel
     * 
     * @param sessInfo
     */
    private void ParentCleanup(WebCacheSessionInfo sessInfo)
    {
        try {
            if (sessInfo.parentChannel != null) sessInfo.parentChannel.close();
        }

        catch (Exception exn) {
            logger.error("Exception during parent cleanup", exn);
        }

        sessInfo.parentChannel = null;
    }

    /**
     * Called to close and cleanup the squid channel
     * 
     * @param sessInfo
     *        The session info
     */
    private void SquidCleanup(WebCacheSessionInfo sessInfo)
    {
        try {
            if (sessInfo.squidKey != null) sessInfo.squidKey.cancel();
            if (sessInfo.squidSelector != null) sessInfo.squidSelector.close();
            if (sessInfo.squidChannel != null) sessInfo.squidChannel.close();
        }

        catch (Exception exn) {
            logger.error("Exception during squid cleanup", exn);
        }

        sessInfo.squidKey = null;
        sessInfo.squidSelector = null;
        sessInfo.squidChannel = null;
    }
}
