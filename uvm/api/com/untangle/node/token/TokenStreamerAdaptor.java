/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.event.TCPStreamer;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Adapts a TokenStreamer to a TCPStreamer.
 *
 */
public class TokenStreamerAdaptor implements TCPStreamer
{
    private final Logger logger = Logger.getLogger(getClass());

    private final TokenStreamer streamer;
    private final NodeTCPSession session;
    
    public TokenStreamerAdaptor( TokenStreamer streamer, NodeTCPSession session )
    {
        this.streamer = streamer;
        this.session = session;
    }

    // TCPStreamer methods ----------------------------------------------------

    public boolean closeWhenDone()
    {
        return streamer.closeWhenDone();
    }

    public ByteBuffer nextChunk()
    {
        logger.debug("streaming next chunk");
        Token tok = streamer.nextToken();

        if (null == tok) {
            return null;
        } else {
            // XXX factor out token writing
            ByteBuffer buf = ByteBuffer.allocate(8);

            Long key = session.getUniqueGlobalAttachmentKey();
            session.globalAttach( key, tok );

            logger.debug("streaming tok: " + tok + " with key: " + key);
            buf.putLong(key);
            buf.flip();
            return buf;
        }
    }
}
