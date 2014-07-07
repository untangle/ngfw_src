/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * An Unparser nodes tokens into bytes.
 *
 */
public interface Unparser
{
    /**
     * Node tokens back into bytes.
     *
     * @param token next token.
     * @return UnparseResult containing byte content of token.
     * @exception UnparseException on unparse error.
     */
    UnparseResult unparse( Token token ) throws UnparseException;

    /**
     * Used for casings that expect byte stream on both sides
     *
     * @param chunk next of data
     * @return UnparseResult containing unparsed content of the chunk
     * @exception UnparseException on unparse error.
     */
    void unparse( NodeTCPSession session, ByteBuffer data ) throws UnparseException;

    /**
     * Called when a session is being released. The unparser should
     * return any queued data.
     *
     * @return Unparse result containing queued data.
     * @exception UnparseException if thrown, it will cause the
     * session to be closed.
     */
    UnparseResult releaseFlush() throws UnparseException;

    /**
     * On session end, the unparser has an opportunity to stream data.
     *
     * @return TokenStreamer that streams the final data.
     */
    TCPStreamer endSession();

    /**
     * Called when both client and server sides
     * {@link com.untangle.uvm.vnet.event.SessionEventListener#handleTCPFinalized are shutdown}
     */
    void handleFinalized();
}
