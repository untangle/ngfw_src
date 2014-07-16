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
     * @return the ParseResult.
     * @exception ParseException if a parse error occurs.
     */
    ParseResult parse( NodeTCPSession session, ByteBuffer chunk ) throws ParseException;

    /**
     * Used for casings that expect byte stream on both sides
     *
     * @param event the TCPChunkEvent received
     * @throws ParseException
     */
    void parseFIXME( NodeTCPSession session, ByteBuffer data ) throws ParseException;

    /**
     * Called with last data from the read buffer on session close.
     *
     * @param chunk data from read buffer.
     * @return the ParseResult.
     * @exception ParseException if a parse error occurs.
     */
    ParseResult parseEnd( NodeTCPSession session, ByteBuffer chunk ) throws ParseException;

    /**
     * On FIN, allows the parser to stream out any final data.
     *
     * XXX this is pretty ugly, I should allow a ParseResult and
     * stream it in the adapt or if necessary.
     *
     * @return a <code>TokenStreamer</code> value
     */
    TokenStreamer endSession( NodeTCPSession session );

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
