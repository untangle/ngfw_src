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
import com.metavize.tran.mail.papi.DoNotCareT;
import com.metavize.tran.mail.papi.DoNotCareChunkT;
import com.metavize.tran.mail.papi.MessageInfo;
import com.metavize.tran.mail.papi.MessageTransmissionTimeoutStrategy;
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
        COMMAND,
        COMMAND_MORE
    };

    public enum ServerState {
        REPLY,
        REPLY_MORE,
        DATA_REPLY,
        DATA_START,
        DATA,
        END_MARKER,
        TRICKLE_START,
        TRICKLE_DATA,
        TRICKLE_END_MARKER,
        DONOTCARE_START,
        DONOTCARE_DATA
    };

    protected File zMsgFile;
    protected MIMEMessageT zMMessageT;
    protected MIMEMessage zMMessage; /* if set, header is null */
    protected MessageInfo zMsgInfo;
    protected long lTimeout;

    private ClientState clientState;
    private ServerState serverState;

    private FileChannel zMsgChannel;
    private MIMEMessageHeaders zMMHeader; /* if set, message is null */

    private long lServerTS;

    // constructors -----------------------------------------------------------

    public PopStateMachine(TCPSession session)
    {
        super(session);

        clientState = ClientState.COMMAND;
        serverState = ServerState.REPLY;

        zMsgFile = null;
        zMMessageT = null;
        zMMessage = null;
        zMsgInfo = null;

        zMsgChannel = null;
        zMMHeader = null;

        updateServerTS();
    }

    // abstract methods -------------------------------------------------------

    protected abstract TokenResult scanMessage() throws TokenException;

    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        //logger.debug("current state: " + clientState);
        clientState = nextClientState(token);
        //logger.debug("next state: " + clientState);

        TokenResult zResult;

        switch (clientState) {
        case COMMAND:
            zResult = doPopCommand((PopCommand) token);
            break;

        case COMMAND_MORE:
            zResult = doPopCommandMore((PopCommandMore) token);
            break;

        default:
            throw new IllegalStateException("unexpected state: " + clientState);
        }

        return zResult;
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        //logger.debug("current state: " + serverState + ", " + token.toString());
        serverState = nextServerState(token);
        //logger.debug("next state: " + serverState);

        TokenResult zResult;

        switch (serverState) {
        case REPLY:
        case DATA_REPLY:
            zResult = doPopReply((PopReply) token);
            break;

        case REPLY_MORE:
            zResult = doPopReplyMore((PopReplyMore) token);
            break;

        case DATA_START:
            zResult = doMIMEMessage((MIMEMessageT) token);
            break;

        case DATA:
            zResult = doMIMEMessageChunk((Chunk) token);
            break;

        case END_MARKER:
            zResult = doMIMEMessageEnd((EndMarker) token);
            break;

        case TRICKLE_START:
            zResult = doMIMEMessageTrickle((MIMEMessageTrickleT) token);
            break;

        case TRICKLE_DATA:
            zResult = doMIMEMessageTrickleChunk((Chunk) token);
            break;

        case TRICKLE_END_MARKER:
            zResult = doMIMEMessageTrickleEnd((EndMarker) token);
            break;

        case DONOTCARE_START:
            zResult = doDoNotCare((DoNotCareT) token);
            break;

        case DONOTCARE_DATA:
            zResult = doDoNotCareChunk((DoNotCareChunkT) token);
            break;

        default:
            throw new IllegalStateException("unexpected state: " + serverState);
        }

        return zResult;
    }

    protected TokenResult doPopCommand(PopCommand cmdT) throws TokenException
    {
        return new TokenResult(null, new Token[] { cmdT });
    }

    protected TokenResult doPopCommandMore(PopCommandMore cmdMoreT) throws TokenException
    {
        return new TokenResult(null, new Token[] { cmdMoreT });
    }

    protected TokenResult doPopReply(PopReply replyT) throws TokenException
    {
        updateServerTS();
        return new TokenResult(new Token[] { replyT }, null);
    }

    protected TokenResult doPopReplyMore(PopReplyMore replyMoreT) throws TokenException
    {
        return new TokenResult(new Token[] { replyMoreT }, null);
    }

    protected TokenResult doMIMEMessage(MIMEMessageT zMMessageT) throws TokenException
    {
        if (true == trickleNow(lTimeout, lServerTS)) {
            MIMEMessageTrickleT zMMTrickleT = new MIMEMessageTrickleT(zMMessageT);
            //logger.debug("trickling message: " + zMMessageT.toString() + ", " + zMMTrickleT.toString() + ", " + this);
            serverState = ServerState.TRICKLE_START;
            //logger.debug("next state: " + serverState);
            return new TokenResult(new Token[] { zMMTrickleT }, null);
        }

        this.zMMessageT = zMMessageT;
        zMsgFile = zMMessageT.getFile();

        zMMHeader = zMMessageT.getMIMEMessageHeader();
        if (null == zMMHeader) {
            /* message has already been re-assembled and can be scanned */
            zMMessage = zMMessageT.getMIMEMessage();
            zMsgInfo = zMMessageT.getMessageInfo();

            TokenResult zResult = scanMessage();

            resetServer();

            return zResult;
        }
        /* else message needs to be re-assembled */

        try {
            zMsgChannel = new FileOutputStream(zMsgFile, true).getChannel();
        } catch (IOException exn) {
            resetServer();
            logger.warn("message file does not exist: ", exn);
        }

        return TokenResult.NONE;
    }

    protected TokenResult doMIMEMessageChunk(Chunk zChunkT) throws TokenException
    {
        if (null != zMMessage) {
            return TokenResult.NONE; //XXXX ???
        }

        if (null != zMMessageT &&
            true == trickleNow(lTimeout, lServerTS)) {
            MIMEMessageTrickleT zMMTrickleT = new MIMEMessageTrickleT(zMMessageT);
            //logger.debug("trickling message w/chunk: " + zMMessageT.toString() + ", " + zMMTrickleT.toString() + ", " + this);
            zMMessageT = null;
            serverState = ServerState.TRICKLE_DATA;
            //logger.debug("next state: " + serverState);
            return new TokenResult(new Token[] { zMMTrickleT, zChunkT }, null);
        }

        return writeFile(zChunkT.getData());
    }

    protected TokenResult doMIMEMessageEnd(EndMarker zEndMarkerT) throws TokenException
    {
        if (null != zMMessage) {
            return TokenResult.NONE; //XXXX ???
        }

        if (null != zMMessageT &&
            true == trickleNow(lTimeout, lServerTS)) {
            MIMEMessageTrickleT zMMTrickleT = new MIMEMessageTrickleT(zMMessageT);
            //logger.debug("trickling message w/marker: " + zMMessageT.toString() + ", " + zMMTrickleT.toString() + ", " + this);
            zMMessageT = null;
            serverState = ServerState.TRICKLE_END_MARKER;
            //logger.debug("next state: " + serverState);
            return new TokenResult(new Token[] { zMMTrickleT, zEndMarkerT }, null);
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

        resetServer();

        return zResult;
    }

    protected TokenResult doMIMEMessageTrickle(MIMEMessageTrickleT zMMTrickleT) throws TokenException
    {
        //logger.debug("trickling message (contd): " + zMMTrickleT.toString());
        return new TokenResult(new Token[] { zMMTrickleT }, null);
    }

    protected TokenResult doMIMEMessageTrickleChunk(Chunk zChunkT) throws TokenException
    {
        //logger.debug("trickling chunk (contd): " + zChunkT.toString());
        return new TokenResult(new Token[] { zChunkT }, null);
    }

    protected TokenResult doMIMEMessageTrickleEnd(EndMarker zEndMarkerT) throws TokenException
    {
        //logger.debug("trickling marker (contd): " + zEndMarkerT.toString());
        return new TokenResult(new Token[] { zEndMarkerT }, null);
    }

    protected TokenResult doDoNotCare(DoNotCareT zDoNotCareT) throws TokenException
    {
        if (null == zMMessageT) {
            return new TokenResult(new Token[] { zDoNotCareT }, null);
        }
        /* else change MIMEMessageT,
         * that leads DoNotCareT,
         * into MIMEMessageTrickleT and
         * push both on pipeline again
         */

        MIMEMessageTrickleT zMMTrickleT = new MIMEMessageTrickleT(zMMessageT);

        resetServer();

        return new TokenResult(new Token[] { zMMTrickleT, zDoNotCareT }, null);
    }

    protected TokenResult doDoNotCareChunk(DoNotCareChunkT zDoNotCareChunkT) throws TokenException
    {
        return new TokenResult(new Token[] { zDoNotCareChunkT }, null);
    }

    // private methods --------------------------------------------------------

    private final void updateServerTS()
    {
        lServerTS = System.currentTimeMillis();
        return;
    }

    private boolean trickleNow(long lTimeout, long lLastTS)
    {
        return MessageTransmissionTimeoutStrategy.inTimeoutDanger(lTimeout, lLastTS);
    }

    private ClientState nextClientState(Token token) throws TokenException
    {
        switch (clientState) {
        case COMMAND:
        case COMMAND_MORE:
            if (token instanceof PopCommand) {
                clientState = ClientState.COMMAND;
            } else if (token instanceof PopCommandMore) {
                clientState = ClientState.COMMAND_MORE;
            } else {
                throw new TokenException("cur: " + clientState + ", next: bad token: " + token.toString());
            }

            return clientState;

        default:
            throw new IllegalStateException("bad state: " + clientState);
        }
    }

    private ServerState nextServerState(Token token) throws TokenException
    {
        switch (serverState) {
        case REPLY:
            if (token instanceof PopReply) {
                if (true == ((PopReply) token).isMsgData()) {
                    serverState = ServerState.DATA_REPLY;
                } else {
                    serverState = ServerState.REPLY;
                }
            } else if (token instanceof PopReplyMore) {
                serverState = ServerState.REPLY_MORE;
            } else if (token instanceof DoNotCareT) {
                serverState = ServerState.DONOTCARE_START;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case REPLY_MORE:
            if (token instanceof PopReply) {
                if (true == ((PopReply) token).isMsgData()) {
                    serverState = ServerState.DATA_REPLY;
                } else {
                    serverState = ServerState.REPLY;
                }
            } else if (token instanceof PopReplyMore) {
                serverState = ServerState.REPLY_MORE;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case DATA_REPLY:
            if (token instanceof MIMEMessageT) {
                serverState = ServerState.DATA_START;
            } else if (token instanceof MIMEMessageTrickleT) {
                serverState = ServerState.TRICKLE_START;
            } else if (token instanceof DoNotCareT) {
                serverState = ServerState.DONOTCARE_START;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case DATA_START:
            if (token instanceof Chunk) {
                serverState = ServerState.DATA;
            } else if (token instanceof EndMarker) {
                serverState = ServerState.END_MARKER;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case DATA:
            if (token instanceof Chunk) {
                serverState = ServerState.DATA;
            } else if (token instanceof EndMarker) {
                serverState = ServerState.END_MARKER;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case END_MARKER:
        case TRICKLE_END_MARKER:
            if (token instanceof PopReply) {
                if (true == ((PopReply) token).isMsgData()) {
                    serverState = ServerState.DATA_REPLY;
                } else {
                    serverState = ServerState.REPLY;
                }
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case TRICKLE_START:
            if (token instanceof Chunk) {
                serverState = ServerState.TRICKLE_DATA;
            } else if (token instanceof EndMarker) {
                serverState = ServerState.TRICKLE_END_MARKER;
            } else if (token instanceof DoNotCareT) {
                serverState = ServerState.DONOTCARE_START;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case TRICKLE_DATA:
            if (token instanceof Chunk) {
                serverState = ServerState.TRICKLE_DATA;
            } else if (token instanceof EndMarker) {
                serverState = ServerState.TRICKLE_END_MARKER;
            } else {
                throw new TokenException("cur: " + serverState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case DONOTCARE_START:
            serverState = ServerState.DONOTCARE_DATA;
            return serverState;

        case DONOTCARE_DATA:
            /* once we enter DONOTCARE stage, we cannot exit */
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
                resetServer();
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

    private void resetClient()
    {
        clientState = ClientState.COMMAND;
        return;
    }

    private void resetServer()
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
