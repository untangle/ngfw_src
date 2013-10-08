/**
 * $Id: TokenHandler.java 34627 2013-05-03 18:30:42Z dmorris $
 */
package com.untangle.node.token;

/**
 * Handles a stream of tokens for a session.
 */
public interface TokenHandler
{
    TokenResult handleClientToken(Token token) throws TokenException;
    TokenResult handleServerToken(Token token) throws TokenException;

    /**
     * Handle a FIN from the client.  By default, this FIN
     * does <b>not</b> result in the shutdown of the server.
     * To accomplish this (to logically propigate the
     * shutdown) call <code>m_myTCPSession.shutdownServer()</code>
     */
    void handleClientFin() throws TokenException;
    void handleServerFin() throws TokenException;
    void handleTimer() throws TokenException;
    void handleFinalized() throws TokenException;

    TokenResult releaseFlush();
}
