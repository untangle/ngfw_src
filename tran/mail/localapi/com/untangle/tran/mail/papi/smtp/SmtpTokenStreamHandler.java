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

package com.untangle.tran.mail.papi.smtp;


import com.untangle.tran.mail.papi.BeginMIMEToken;
import com.untangle.tran.mail.papi.CompleteMIMEToken;
import com.untangle.tran.mail.papi.ContinuedMIMEToken;
import com.untangle.tran.token.Chunk;
import com.untangle.tran.token.TokenResultBuilder;



/**
 * Base class for Object wishing to listen-on
 * a raw Smtp Token Stream.  Instances become
 * registered on a Stream by associating
 * itself either in the constructor
 * or the "setHandler" method
 * of a {@link com.untangle.tran.mail.papi.smtp.SmtpTokenStream SmtpTokenStream}.
 * <br>
 * Note if the handler pukes in a horrible
 * way that you want all other
 * transforms to ignore this session, make sure to
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
