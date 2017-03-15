/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.TCPStreamer;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * Adapts a TokenStreamer to a TCPStreamer.
 *
 */
public class TokenStreamerAdaptor implements TCPStreamer
{
    private final Logger logger = Logger.getLogger(getClass());

    private final TokenStreamer streamer;
    private final AppTCPSession session;
    
    public TokenStreamerAdaptor( TokenStreamer streamer, AppTCPSession session )
    {
        this.streamer = streamer;
        this.session = session;
    }

    // TCPStreamer methods ----------------------------------------------------

    public boolean closeWhenDone()
    {
        return streamer.closeWhenDone();
    }

    public Object nextChunk()
    {
        logger.debug("streaming next chunk");
        return streamer.nextToken();
    }
}
