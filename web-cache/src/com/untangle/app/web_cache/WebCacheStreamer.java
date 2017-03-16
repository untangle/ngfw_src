package com.untangle.app.web_cache; // IMPL

import java.nio.ByteBuffer;
import com.untangle.uvm.vnet.TCPStreamer;
import org.apache.log4j.Logger;

public class WebCacheStreamer implements TCPStreamer
{
    private final Logger logger = Logger.getLogger(getClass());
    private final WebCacheSessionInfo sessInfo;
    private final WebCacheApp app;
    private int total = 0;

    public WebCacheStreamer(WebCacheApp app,WebCacheSessionInfo sessInfo)
    {
        logger.debug("WEBCACHE WebCacheStreamer()");
        this.sessInfo = sessInfo;
        this.app = app;
    }

    public ByteBuffer nextChunk()
    {
        ByteBuffer rxbuff = ByteBuffer.allocate(app.STREAM_BUFFSIZE);
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

        if (app.SOCKET_DEBUG == true) logger.debug("WEBCACHE nextChunk received " + retval + " bytes");

        // increment our counters
        total = (total + retval);
        app.statistics.AddHitBytes(retval);

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
