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

package com.untangle.node.smtp;


import com.untangle.node.smtp.BeginMIMEToken;
import com.untangle.node.smtp.CompleteMIMEToken;
import com.untangle.node.smtp.ContinuedMIMEToken;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.TokenResultBuilder;



/**
 * Base class for Object wishing to listen-on
 * a raw Smtp Token Stream.  Instances become
 * registered on a Stream by associating
 * itself either in the constructor
 * or the "setHandler" method
 * of a {@link com.untangle.node.smtp.SmtpTokenStream SmtpTokenStream}.
 * <br>
 * Note if the handler pukes in a horrible
 * way that you want all other
 * nodes to ignore this session, make sure to
 * pass-along Passthru tokens in each direction.
 *
 */
public abstract class SmtpTokenStreamHandler {

    private SmtpTokenStream m_stream;

    /**
     * <b>Only to be called by the SmtpTokenStream passing
     * itself</b>
     */
    protected final void setSmtpTokenStream(SmtpTokenStream stream) {
        m_stream = stream;
    }

    /**
     * Get the Stream which is sending tokens to this
     * instance.  If this instance is not registered
     * with any SmtpTokenStream, this method
     * returns null.
     */
    public final SmtpTokenStream getSmtpTokenStream() {
        return m_stream;
    }

    /**
     * Callback indicating that this conversation
     * is entering passthru mode.  Unlike some of the
     * other methods on this class, the caller should
     * <b>not</b> assume responsibility to re-passing
     * the Passthru token.  This has already been done
     * by the caller.
     * <br>
     * Implementations should pass along any queued
     * client/server data.  This will be the last
     * time the Handler is called for any token-handling
     * methods.
     *
     * @param resultBuilder the builder, to pass along
     *        tokens/streams to the client/server
     */
    public abstract void passthru(TokenResultBuilder resultBuilder);

    /**
     * Handle a Command token
     *
     * @param resultBuilder the builder, to pass along
     *        tokens/streams to the client/server
     *
     * @param cmd the command
     */
    public abstract void handleCommand(TokenResultBuilder resultBuilder,
                                       Command cmd);

    /**
     * Handle a MAILCommand token
     *
     * @param resultBuilder the builder, to pass along
     *        tokens/streams to the client/server
     *
     * @param cmd the command
     */
    public abstract void handleMAILCommand(TokenResultBuilder resultBuilder,
                                           MAILCommand cmd);

    /**
     * Handle a RCPTCommand token
     *
     * @param resultBuilder the builder, to pass along
     *        tokens/streams to the client/server
     *
     * @param cmd the command
     */
    public abstract void handleRCPTCommand(TokenResultBuilder resultBuilder,
                                           RCPTCommand cmd);

    /**
     * Handle a BeginMIMEToken token
     *
     * @param resultBuilder the builder, to pass along
     *        tokens/streams to the client/server
     *
     * @param token the token
     */
    public abstract void handleBeginMIME(TokenResultBuilder resultBuilder,
                                         BeginMIMEToken token);

    /**
     * Handle a ContinuedMIMEToken token
     *
     * @param resultBuilder the builder, to pass along
     *        tokens/streams to the client/server
     *
     * @param token the token
     */
    public abstract void handleContinuedMIME(TokenResultBuilder resultBuilder,
                                             ContinuedMIMEToken token);

    /**
     * Handle a Response token
     *
     * @param resultBuilder the builder, to pass along
     *        tokens/streams to the client/server
     *
     * @param resp the resp
     */
    public abstract void handleResponse(TokenResultBuilder resultBuilder,
                                        Response resp);

    /**
     * Handle a Chunk token from server to client
     *
     * @param resultBuilder the builder, to pass along
     *        tokens/streams to the client/server
     *
     * @param chunk the chunk
     */
    public abstract void handleChunkForClient(TokenResultBuilder resultBuilder,
                                              Chunk chunk);

    /**
     * Handle a Chunk token from client to server
     *
     * @param resultBuilder the builder, to pass along
     *        tokens/streams to the client/server
     *
     * @param chunk the chunk
     */
    public abstract void handleChunkForServer(TokenResultBuilder resultBuilder,
                                              Chunk chunk);

    /**
     * Handle a CompleteMIME token
     *
     * @param resultBuilder the builder, to pass along
     *        tokens/streams to the client/server
     *
     * @param token the token
     */
    public abstract void handleCompleteMIME(TokenResultBuilder resultBuilder,
                                            CompleteMIMEToken token);

    /**
     * Handle a FIN from the server.
     *
     * @return true if the client should be shutdown.  False
     *         to leave the client side open.
     */
    public abstract boolean handleServerFIN();


    /**
     * Handle a FIN from the client.
     *
     * @return true if the server should be shutdown.  False
     *         to leave the server side open.
     */
    public abstract boolean handleClientFIN();

    /**
     * Called when both client and server sides are closed.  Any
     * associated resources should be closed and any interesting logging
     * made.
     */
    public abstract void handleFinalized();

}
