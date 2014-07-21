/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * An Unparser nodes tokens into bytes.
 *
 */
public interface Unparser
{
    void handleNewSession( NodeTCPSession session );

    /**
     * Unparse tokens back into bytes.
     *
     * @param token next token.
     */
    void unparse( NodeTCPSession session, Token token ) throws Exception;

    /**
     * Unparse bytes back into bytes
     * This is currently used by the HTTPS inspector
     * to unparse unecrypted text back into SSL
     */
    void unparse( NodeTCPSession session, ByteBuffer buffer );
    
    /**
     * Called when a session is being released. The unparser should
     * return any queued data.
     *
     * @exception UnparseException if thrown, it will cause the
     * session to be closed.
     */
    void releaseFlush( NodeTCPSession session ) throws Exception;

    /**
     * On session end, the unparser has an opportunity to stream data.
     */
    void endSession( NodeTCPSession session );

    /**
     * Called when both client and server sides
     * {@link com.untangle.uvm.vnet.SessionEventListener#handleTCPFinalized are shutdown}
     */
    void handleFinalized( NodeTCPSession session );
}
