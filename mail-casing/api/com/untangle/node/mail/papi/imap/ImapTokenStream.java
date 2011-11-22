/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.mail.papi.imap;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.ContinuedMIMEToken;
import com.untangle.node.token.AbstractTokenHandler;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.PassThruToken;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenResult;
import com.untangle.uvm.vnet.TCPSession;


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
     * method the {@link com.untangle.node.mail.papi.imap.ImapTokenStreamHandler#getStream getStream}
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


