/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;
import com.untangle.uvm.vnet.event.TCPChunkEvent;

/**
 * Parses a stream of bytes into tokens.
 *
 */
public interface Parser
{
    /**
     * Parse data from the stream.
     *
     * @param chunk the byte data from the stream.
     * @return the ParseResult.
     * @exception ParseException if a parse error occurs.
     */
    ParseResult parse(ByteBuffer chunk) throws ParseException;

    /**
     * Used for casings that expect byte stream on both sides
     *
     * @param event the TCPChunkEvent received
     * @return the ByteBuffer to return
     * @throws ParseException
     */
    void parse(TCPChunkEvent event) throws ParseException;

    /**
     * Called with last data from the read buffer on session close.
     *
     * @param chunk data from read buffer.
     * @return the ParseResult.
     * @exception ParseException if a parse error occurs.
     */
    ParseResult parseEnd(ByteBuffer chunk) throws ParseException;

    /**
     * On FIN, allows the parser to stream out any final data.
     *
     * XXX this is pretty ugly, I should allow a ParseResult and
     * stream it in the adapt or if necessary.
     *
     * @return a <code>TokenStreamer</code> value
     */
    TokenStreamer endSession();

    /**
     * Called on scheduled timer event.
     */
    void handleTimer();

    /**
     * Called when both client and server sides
     * {@link com.untangle.uvm.vnet.event.SessionEventListener#handleTCPFinalized are shutdown}
     */
    void handleFinalized();
}
