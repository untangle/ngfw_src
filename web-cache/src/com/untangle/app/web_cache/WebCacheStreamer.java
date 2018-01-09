/**
 * $Id$
 */

package com.untangle.app.web_cache;

import java.nio.ByteBuffer;
import com.untangle.uvm.vnet.TCPStreamer;
import org.apache.log4j.Logger;

/**
 * This is our streamer class that is used to get cached content from Squid and
 * stream it to the client.
 */
public class WebCacheStreamer implements TCPStreamer
{
    private final Logger logger = Logger.getLogger(getClass());
    private final WebCacheSessionInfo sessInfo;
    private final WebCacheApp app;
    private int total = 0;

    /**
     * Constructor
     * 
     * @param app
     *        The application that created uss
     * @param sessInfo
     *        The session information
     */
    public WebCacheStreamer(WebCacheApp app, WebCacheSessionInfo sessInfo)
    {
        logger.debug("WEBCACHE WebCacheStreamer()");
        this.sessInfo = sessInfo;
        this.app = app;
    }

    /**
     * Reads chunks of data from Squid and returns them to be sent to the
     * client.
     * 
     * @return A buffer of data to send, or null when finished
     */
    public ByteBuffer nextChunk()
    {
        ByteBuffer rxbuff = ByteBuffer.allocate(app.STREAM_BUFFSIZE);
        int retval = 0;

        try {
            retval = sessInfo.squidChannel.read(rxbuff);
        }

        catch (Exception exn) {
            logger.error("Error reading from cache", exn);
        }

        // squid is configured to close the socket once all the
        // data is returned at which point we are finished
        if (retval < 0) return (null);

        // this should never happen since we are in blocking
        // mode but we anticipate and handle it anyhow
        if (retval == 0) return (null);

        if (app.SOCKET_DEBUG == true) logger.debug("WEBCACHE nextChunk received " + retval + " bytes");

        // increment our counters
        total = (total + retval);
        app.statistics.AddHitBytes(retval);

        // flip the buffer and return to client
        rxbuff.flip();
        return (rxbuff);
    }

    /**
     * Called to get our close when done flag
     * 
     * @return False so we can handle the cleanup ourselves
     */
    public boolean closeWhenDone()
    {
        logger.debug("WEBCACHE closeWhenDone()");
        logger.debug("----- CLIENT RECEIVED " + total + " BTYES FROM SQUID -----");
        return (false);
    }
}
