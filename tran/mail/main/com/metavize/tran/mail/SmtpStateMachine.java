/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractTokenHandler;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import org.apache.log4j.Logger;

public abstract class SmtpStateMachine extends AbstractTokenHandler
{
    private final Logger logger = Logger.getLogger(SmtpStateMachine.class);

    public enum ClientState {
        COMMAND,
        DATA_CMD,
        DATA,
        MESSAGE_FILE,
        END_MARKER
    };

    public enum ServerState {
        REPLY
    };

    private ClientState clientState = ClientState.COMMAND;

    private MimeStateMachine mimeStateMachine = null;

    // constructors -----------------------------------------------------------

    public SmtpStateMachine(TCPSession session)
    {
        super(session);
    }

    // abstract methods -------------------------------------------------------

    protected abstract TokenResult doSmtpCommand(SmtpCommand cmd)
        throws TokenException;
    protected abstract TokenResult doMessageHeader(Rfc822Header header)
        throws TokenException;
    protected abstract TokenResult doBodyChunk(Chunk chunk)
        throws TokenException;
    protected abstract TokenResult doPreamble(Chunk chunk)
        throws TokenException;
    protected abstract TokenResult doBoundary(MimeBoundary boundary,
                                              boolean end)
        throws TokenException;
    protected abstract TokenResult doMultipartHeader(Rfc822Header header)
        throws TokenException;
    protected abstract TokenResult doMultipartBody(Chunk chunk)
        throws TokenException;
    protected abstract TokenResult doEpilogue(Chunk chunk)
        throws TokenException;
    protected abstract TokenResult doMessageFile(MessageFile messageFile)
        throws TokenException;
    protected abstract TokenResult doMessageEnd(EndMarker endMarker)
        throws TokenException;

    protected abstract TokenResult doSmtpReply(SmtpReply reply)
        throws TokenException;

    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        clientState = nextClientState(token);

        switch (clientState) {
        case COMMAND:
            return doSmtpCommand((SmtpCommand)token);

        case DATA_CMD:
            return doSmtpCommand((SmtpCommand)token);

        case DATA:
            MimeStateMachine.State state = mimeStateMachine.nextState(token);
            logger.debug("state: " + state);

            switch (state) {
            case MESSAGE_HEADER:
                return doMessageHeader((Rfc822Header)token);

            case BODY:
                return doBodyChunk((Chunk)token);

            case PREAMBLE:
                return doPreamble((Chunk)token);

            case BOUNDARY:
                return doBoundary((MimeBoundary)token, false);

            case MULTIPART_HEADER:
                return doMultipartHeader((Rfc822Header)token);

            case MULTIPART_BODY:
                return doMultipartBody((Chunk)token);

            case END_BOUNDARY:
                return doBoundary((MimeBoundary)token, true);

            case EPILOGUE:
                return doEpilogue((Chunk)token);

            default:
                throw new IllegalStateException("unexpected state: " + state);
            }

        case MESSAGE_FILE:
            return doMessageFile((MessageFile)token);

        case END_MARKER:
            return doMessageEnd((EndMarker)token);

        default:
            throw new IllegalStateException("unexpected state: " + clientState);
        }
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        return doSmtpReply((SmtpReply)token);
    }

    // private methods --------------------------------------------------------

    private ClientState nextClientState(Token token) throws TokenException
    {
        switch (clientState) {
        case COMMAND:
            if (token instanceof SmtpCommand) {
                SmtpCommand cmd = (SmtpCommand)token;

                if (cmd.getCommand().equals("DATA")) {
                    mimeStateMachine = new MimeStateMachine();
                    clientState = ClientState.DATA_CMD;
                }

            } else {
                throw new TokenException("bad token: " + token.getClass());
            }

            return clientState;

        case DATA_CMD:
            if (token instanceof MessageFile) {
                clientState = ClientState.MESSAGE_FILE;
            } else {
                clientState = ClientState.DATA;
            }

            return clientState;

        case DATA:
            if (token instanceof MessageFile) {
                clientState = ClientState.MESSAGE_FILE;
            }

            return clientState;

        case MESSAGE_FILE:
            if (token instanceof EndMarker) {
                clientState = ClientState.END_MARKER;
            } else {
                throw new TokenException("bad token: " + token.getClass());
            }

            return clientState;

        case END_MARKER:
            if (token instanceof SmtpCommand) {
                clientState = ClientState.COMMAND;
            } else {
                throw new TokenException("bad token: " + token.getClass());
            }

            return clientState;

        default:
            throw new IllegalStateException("bad state: " + clientState);
        }
    }
}
