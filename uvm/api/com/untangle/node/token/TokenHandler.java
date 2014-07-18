/**
 * $Id$
 */
package com.untangle.node.token;

import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Handles a stream of tokens for a session.
 */
public interface TokenHandler
{
    void handleClientToken( NodeTCPSession session, Token token );
    void handleServerToken( NodeTCPSession session, Token token);

    void handleNewSessionRequest( TCPNewSessionRequest tsr ) ;
    void handleNewSession( NodeTCPSession session ) ;

    void handleClientFin( NodeTCPSession session );
    void handleServerFin( NodeTCPSession session );
    void handleTimer( NodeSession session );
    void handleFinalized( NodeTCPSession session );

    void releaseFlush( NodeTCPSession session );
}
