/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.papi.imap;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.mail.papi.ContinuedMIMEToken;
import com.untangle.tran.token.AbstractTokenHandler;
import com.untangle.tran.token.Chunk;
import com.untangle.tran.token.PassThruToken;
import com.untangle.tran.token.Token;
import com.untangle.tran.token.TokenResult;
import org.apache.log4j.Logger;


/**
 * Class representing a tap into a stream of
 * Imap tokens.  This class is used via the
 * {@link #setHandler ImapTokenStreamHandler} subclass
 * you provide.
 */
public final class ImapTokenStream
    extends AbstractTokenHandler {

    private final Logger m_logger =
        Logger.getLogger(ImapTokenStream.class);

    private ImapTokenStreamHandler m_handler;
    private long m_lastClientTimestamp;
    private long m_lastServerTimestamp;

    /**
     * Construct an ImapTokenStream with an initial
     * passthru handler.
     *
     * @param session the TCPSession
     * @param handler the handler for callbacks
     */
    public ImapTokenStream(TCPSession session) {
        this(session, new PassthruImapTokenStreamHandler());
    }

    /**
     * Construct an ImapTokenStream
     *
     * @param session the TCPSession
     * @param handler the handler for callbacks
     */
    public ImapTokenStream(TCPSession session,
                           ImapTokenStreamHandler handler) {
        super(session);
        updateTimestamps(true, true);
        setHandler(handler);
    }

    /**
     * Access the underlying TCPSession for this TokenStream
     *
     * @return the underlying TCPSession
     */
    public TCPSession getTCPSession() {
        return getSession();
    }

    /**
     * Set the ImapTokenStreamHandler for this Stream.  After calling this
     * method the {@link com.untangle.tran.mail.papi.imap.ImapTokenStreamHandler#getStream getStream}
     * of the handler will return this object.  Any previous handler will have its
     * stream set to null.
     * <br><br>
     * Passing null implicitly unregisters any existing handler, and uses
     * a {@link PassthruImapTokenStreamHandler PassthruImapTokenStreamHandler}.
     *
     * @param handler the handler.
     */
    public void setHandler(ImapTokenStreamHandler handler) {
        if(handler == null) {
            handler = new PassthruImapTokenStreamHandler();
        }
        if(m_handler != null) {
            m_handler.setStream(null);
        }
        m_handler = handler;
        m_handler.setStream(this);
    }

    /**
     * Accessor for the underlying handler of this
     * Stream's token events.
     */
    public ImapTokenStreamHandler getHandler() {
        return m_handler;
    }

    /**
     * Get the time (in local clock time) of the last transmission
     * to/from the client.
     */
    public long getLastClientTimestamp() {
        return m_lastClientTimestamp;
    }
    /**
     * Get the time (in local clock time) of the last transmission
     * to/from the server.
     */
    public long getLastServerTimestamp() {
        return m_lastServerTimestamp;
    }

    //FROM client
    public TokenResult handleClientToken(Token token) {
        TokenResult ret = handleClientTokenImpl(token);
        if(ret != null) {
            updateTimestamps(true, ret.hasDataForServer());
        }
        else {
            updateTimestamps(true, false);
        }
        return ret;
    }

    //FROM server
    public TokenResult handleServerToken(Token token) {
        TokenResult ret = handleServerTokenImpl(token);
        if(ret != null) {
            updateTimestamps(ret.hasDataForClient(), true);
        }
        else {
            updateTimestamps(false, true);
        }
        return ret;
    }

    private void updateTimestamps(boolean client, boolean server) {
        long now = System.currentTimeMillis();
        if(client) {
            m_lastClientTimestamp = now;
        }
        if(server) {
            m_lastServerTimestamp = now;
        }
    }

    //FROM client
    private TokenResult handleClientTokenImpl(Token token) {
        return new TokenResult(null, new Token[] { token });
    }

    //FROM server
    private TokenResult handleServerTokenImpl(Token token) {
        if(token instanceof PassThruToken) {
            m_logger.debug("Received PASSTHRU token");
            return new TokenResult(new Token[] { token }, null);
        }
        if(token instanceof UnparsableMIMEChunk) {
            m_logger.debug("Received UnparsableMIMEChunk");
            return new TokenResult(new Token[] { token }, null);
        }
        if(token instanceof ImapChunk) {
            m_logger.debug("Received ImapChunk");
            return m_handler.handleChunkFromServer((ImapChunk) token);
        }
        if(token instanceof Chunk) {
            m_logger.debug("Received Chunk (assume passthru)");
            return new TokenResult(new Token[] { token }, null);
        }
        if(token instanceof BeginImapMIMEToken) {
            m_logger.debug("Received BeginImapMIMEToken");
            return m_handler.handleBeginMIMEFromServer((BeginImapMIMEToken) token);
        }
        if(token instanceof ContinuedMIMEToken) {
            m_logger.debug("Received ContinuedMIMEToken");
            return m_handler.handleContinuedMIMEFromServer((ContinuedMIMEToken) token);
        }
        if(token instanceof CompleteImapMIMEToken) {
            m_logger.debug("Received CompleteImapMIMEToken");
            return m_handler.handleCompleteMIMEFromServer((CompleteImapMIMEToken) token);
        }

        m_logger.error("Unknown token type: " + token);
        return new TokenResult(new Token[] { token }, null);
    }

    public void handleClientFin() {
        if(m_handler.handleClientFin()) {
            getTCPSession().shutdownServer();
        }
    }
    public void handleServerFin() {
        if(m_handler.handleServerFin()) {
            getTCPSession().shutdownClient();
        }
    }
    public void handleFinalized() {
        m_handler.handleFinalized();
    }

}


