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

package com.metavize.tran.mail.papi.pop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.MessageInfo;
import com.metavize.tran.mail.papi.MIMEMessageT;
import com.metavize.tran.mail.papi.MIMEMessageTrickleT;
import com.metavize.tran.mime.InvalidHeaderDataException;
import com.metavize.tran.mime.HeaderParseException;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.mime.MIMEMessageHeaders;
import com.metavize.tran.mime.MIMEPartParseException;
import com.metavize.tran.mime.MIMEPolicy;
import com.metavize.tran.token.AbstractTokenHandler;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import org.apache.log4j.Logger;

public abstract class PopStateMachine extends AbstractTokenHandler
{
    private final static Logger logger = Logger.getLogger(PopStateMachine.class);

    /* documented as fyi - not explicitly used */
    public enum ClientState {
        COMMAND
    };

    public enum ServerState {
        REPLY,
        DATA_REPLY,
        DATA_START,
        DATA,
        END_MARKER,
        TRICKLE_START,
        TRICKLE_DATA,
        TRICKLE_END_MARKER
    };

    protected File zMsgFile;
    protected MIMEMessageT zMMessageT;
    protected MIMEMessage zMMessage; /* if set, header is null */
    protected MessageInfo zMsgInfo;

    private ServerState serverState;

    private FileChannel zMsgChannel;
    private MIMEMessageHeaders zMMHeader; /* if set, message is null */

    // constructors -----------------------------------------------------------

    public PopStateMachine(TCPSession session)
    {
        super(session);

        serverState = ServerState.REPLY;

        zMsgFile = null;
        zMMessageT = null;
        zMMessage = null;
        zMsgInfo = null;

        zMsgChannel = null;
        zMMHeader = null;
    }

    // abstract methods -------------------------------------------------------

    protected abstract TokenResult scanMessage() throws TokenException;

    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        return doPopCommand((PopCommand)token);
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        //logger.debug("current state: " + serverState);
        serverState = nextServerState(token);
        //logger.debug("next state: " + serverState);

        switch (serverState) {
        case REPLY:
        case DATA_REPLY:
            return doPopReply((PopReply) token);

        case DATA_START:
            return doMIMEMessage((MIMEMessageT) token);

        case DATA:
            return doMIMEMessageChunk((Chunk) token);

        case END_MARKER:
            return doMIMEMessageEnd((EndMarker) token);

        case TRICKLE_START:
            return doMIMEMessageTrickle((MIMEMessageTrickleT) token);

        case TRICKLE_DATA:
            return doMIMEMessageTrickleChunk((Chunk) token);

        case TRICKLE_END_MARKER:
            return doMIMEMessageTrickleEnd((EndMarker) token);

        default:
            throw new IllegalStateException("unexpected state: " + serverState);
        }
    }

    protected TokenResult doPopCommand(PopCommand cmdT) throws TokenException
    {
        return new TokenResult(null, new Token[] { cmdT });
    }

    protected TokenResult doPopReply(PopReply replyT) throws TokenException
    {
        return new TokenResult(new Token[] { replyT }, null);
    }

    protected TokenResult doMIMEMessage(MIMEMessageT zMMessageT) throws TokenException
    {
        this.zMMessageT = zMMessageT;
        zMsgFile = zMMessageT.getFile();

        zMMHeader = zMMessageT.getMIMEMessageHeader();
        if (null == zMMHeader) {
            /* message has already been re-assembled and can be scanned */
            zMMessage = zMMessageT.getMIMEMessage();
            zMsgInfo = zMMessageT.getMessageInfo();

            TokenResult zResult = scanMessage();

            reset();

            return zResult;
        }
        /* else message needs to be re-assembled */

        try {
            zMsgChannel = new FileOutputStream(zMsgFile, true).getChannel();
        } catch (IOException exn) {
            reset();
            logger.warn("message file does not exist: ", exn);
        }

        return TokenResult.NONE;
    }

    protected TokenResult doMIMEMessageChunk(Chunk zChunkT) throws TokenException
    {
        if (null != zMMessage) {
            return TokenResult.NONE; //XXXX ???
        }

        return writeFile(zChunkT.getData());
    }

    protected TokenResult doMIMEMessageEnd(EndMarker zEndMarkerT) throws TokenException
    {
        if (null != zMMessage) {
            return TokenResult.NONE; //XXXX ???
        }

        try {
            zMMessage = new MIMEMessage(zMMessageT.getInputStream(), zMMessageT.getFileMIMESource(), new MIMEPolicy(), null, zMMHeader);
        } catch (IOException exn) {
            logger.warn("cannot get FileMIMESource MIMEParsingInputStream: ", exn);
            return TokenResult.NONE; //XXXX ???
        } catch (InvalidHeaderDataException exn) {
            logger.warn("cannot create MIME message: ", exn);
            return TokenResult.NONE; //XXXX ???
        } catch (HeaderParseException exn) {
            logger.warn("cannot create MIME message: ", exn);
            return TokenResult.NONE; //XXXX ???
        } catch (MIMEPartParseException exn) {
            logger.warn("cannot create MIME message: ", exn);
            return TokenResult.NONE; //XXXX ???
        }

        zMMessageT.setMIMEMessage(zMMessage);
        zMMessageT.setMIMEMessageHeader(null); /* discard header */
        zMMHeader = null;

        zMsgInfo = zMMessageT.getMessageInfo();

        TokenResult zResult = scanMessage();

        reset();

        return zResult;
    }

    protected TokenResult doMIMEMessageTrickle(MIMEMessageTrickleT zMMTrickleT) throws TokenException
    {
        return new TokenResult(new Token[] { zMMTrickleT }, null);
    }

    protected TokenResult doMIMEMessageTrickleChunk(Chunk zChunkT) throws TokenException
    {
        return new TokenResult(new Token[] { zChunkT }, null);
    }

    protected TokenResult doMIMEMessageTrickleEnd(EndMarker zEndMarkerT) throws TokenException
    {
        return new TokenResult(new Token[] { zEndMarkerT }, null);
    }

    // private methods --------------------------------------------------------

    private ServerState nextServerState(Token token) throws TokenException
    {
        switch (serverState) {
        case REPLY:
            if (token instanceof PopReply) {
                PopReply reply = (PopReply)token;

                if (true == reply.isMsgData()) {
                    serverState = ServerState.DATA_REPLY;
                }
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.getClass());
            }

            return serverState;

        case DATA_REPLY:
            if (token instanceof MIMEMessageT) {
                serverState = ServerState.DATA_START;
            } else if (token instanceof MIMEMessageTrickleT) {
                serverState = ServerState.TRICKLE_START;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.getClass());
            }

            return serverState;

        case DATA_START:
            if (token instanceof Chunk) {
                serverState = ServerState.DATA;
            } else if (token instanceof EndMarker) {
                serverState = ServerState.END_MARKER;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.getClass());
            }

            return serverState;

        case DATA:
            if (token instanceof Chunk) {
                /* fall through */
            } else if (token instanceof EndMarker) {
                serverState = ServerState.END_MARKER;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.getClass());
            }

            return serverState;

        case END_MARKER:
        case TRICKLE_END_MARKER:
            if (token instanceof PopReply) {
                PopReply reply = (PopReply)token;

                if (true == reply.isMsgData()) {
                    serverState = ServerState.DATA_REPLY;
                } else {
                    serverState = ServerState.REPLY;
                }
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.getClass());
            }

            return serverState;

        case TRICKLE_START:
            if (token instanceof Chunk) {
                serverState = ServerState.TRICKLE_DATA;
            } else if (token instanceof EndMarker) {
                serverState = ServerState.TRICKLE_END_MARKER;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.getClass());
            }

            return serverState;

        case TRICKLE_DATA:
            if (token instanceof Chunk) {
                /* fall through */
            } else if (token instanceof EndMarker) {
                serverState = ServerState.TRICKLE_END_MARKER;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.getClass());
            }

            return serverState;

        default:
            throw new IllegalStateException("bad state: " + serverState);
        }
    }

    private TokenResult writeFile(ByteBuffer zBuf) throws TokenException
    {
        if (null != zMsgChannel) {
            try {
                for (; true == zBuf.hasRemaining(); ) {
                     zMsgChannel.write(zBuf);
                }
            } catch (IOException exn) {
                reset();
                throw new TokenException("cannot write date to message file: ", exn);
            }
        }

        return TokenResult.NONE;
    }

    private void closeMsgChannel()
    {
        if (null == zMsgChannel) {
            return;
        }

        try {
            zMsgChannel.close();
        } catch (IOException exn) {
            logger.warn("cannot close message file: ", exn);
        } finally {
            zMsgChannel = null;
        }

        return;
    }

    private void reset()
    {
        serverState = ServerState.REPLY;

        zMMHeader = null;
        closeMsgChannel();

        zMsgFile = null;
        zMMessageT = null;
        zMMessage = null;
        zMsgInfo = null;

        return;
    }
}
