/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.TCPStreamer;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * Adapts a TokenStreamer to a TCPStreamer.
 */
public class TokenStreamerAdaptor implements TCPStreamer
{
    private final Logger logger = Logger.getLogger(getClass());

    private final TokenStreamer streamer;
    private final AppTCPSession session;
    
    /**
     * Constructor
     * @param streamer
     * @param session
     */
    public TokenStreamerAdaptor( TokenStreamer streamer, AppTCPSession session )
    {
        this.streamer = streamer;
        this.session = session;
    }

    /**
     * retuns true if should be closed when done
     * @return bool
     */
    public boolean closeWhenDone()
    {
        return streamer.closeWhenDone();
    }

    /**
     * Get the next token/chunk
     * @return Object
     */
    public Object nextChunk()
    {
        logger.debug("streaming next chunk");
        return streamer.nextToken();
    }
}
