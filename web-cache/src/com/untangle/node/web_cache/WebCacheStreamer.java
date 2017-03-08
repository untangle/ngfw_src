package com.untangle.node.web_cache; // IMPL

import java.nio.ByteBuffer;
import com.untangle.uvm.vnet.TCPStreamer;
import org.apache.log4j.Logger;

public class WebCacheStreamer implements TCPStreamer
{
    private final Logger logger = Logger.getLogger(getClass());
    private final WebCacheSessionInfo sessInfo;
    private final WebCacheApp node;
    private int total = 0;

    public WebCacheStreamer(WebCacheApp node,WebCacheSessionInfo sessInfo)
    {
        logger.debug("WEBCACHE WebCacheStreamer()");
        this.sessInfo = sessInfo;
        this.node = node;
    }

    public ByteBuffer nextChunk()
    {
        ByteBuffer rxbuff = ByteBuffer.allocate(node.STREAM_BUFFSIZE);
        int retval = 0;

            try
            {
            retval = sessInfo.squidChannel.read(rxbuff);
            }

            catch (Throwable t)
            {
            WebCacheStackDump.error(logger,"WebCacheStreamer","nextChunk()",t);
            }

        // squid is configured to close the socket once all the
        // data is returned at which point we are finished
        if (retval < 0) return(null);

        // this should never happen since we are in blocking
        // mode but we anticipate and handle it anyhow
        if (retval == 0) return(null);

        if (node.SOCKET_DEBUG == true) logger.debug("WEBCACHE nextChunk received " + retval + " bytes");

        // increment our counters
        total = (total + retval);
        node.statistics.AddHitBytes(retval);

        // flip the buffer and return to client
        rxbuff.flip();
        return(rxbuff);
    }

    public boolean closeWhenDone()
    {
        logger.debug("WEBCACHE closeWhenDone()");
        logger.debug("----- CLIENT RECEIVED " + total + " BTYES FROM SQUID -----");
        return(false);
    }
}
