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
    TokenResult handleClientToken( NodeTCPSession session, Token token ) throws TokenException;
    TokenResult handleServerToken( NodeTCPSession session, Token token) throws TokenException;

    void handleNewSessionRequest( TCPNewSessionRequest tsr ) ;
    void handleNewSession( NodeTCPSession session ) ;

    void handleClientFin( NodeTCPSession session ) throws TokenException;
    void handleServerFin( NodeTCPSession session ) throws TokenException;
    void handleTimer( NodeSession session ) throws TokenException;
    void handleFinalized( NodeTCPSession session ) throws TokenException;

    TokenResult releaseFlush( NodeTCPSession session );
}
