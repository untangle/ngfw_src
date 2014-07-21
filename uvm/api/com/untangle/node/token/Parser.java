/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Parses a stream of bytes into tokens.
 *
 */
public interface Parser
{
    void handleNewSession( NodeTCPSession session );
    
    /**
     * Parse data from the stream.
     *
     * @param chunk the byte data from the stream.
     */
    void parse( NodeTCPSession session, ByteBuffer chunk ) throws Exception;

    /**
     * Called with last data from the read buffer on session close.
     *
     * @param chunk data from read buffer.
     * @exception ParseException if a parse error occurs.
     */
    void parseEnd( NodeTCPSession session, ByteBuffer chunk ) throws Exception;

    /**
     * On FIN, allows the parser to stream out any final data.
     *
     * @return a <code>TokenStreamer</code> value
     */
    void endSession( NodeTCPSession session );

    /**
     * Called on scheduled timer event.
     */
    void handleTimer( NodeSession session );

    /**
     * Called when both client and server sides
     * {@link com.untangle.uvm.vnet.SessionEventListener#handleTCPFinalized are shutdown}
     */
    void handleFinalized( NodeTCPSession session );
}
