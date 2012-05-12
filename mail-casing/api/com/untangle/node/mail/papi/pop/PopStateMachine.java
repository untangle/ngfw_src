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

package com.untangle.node.mail.papi.pop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.DoNotCareChunkT;
import com.untangle.node.mail.papi.DoNotCareT;
import com.untangle.node.mail.papi.MIMEMessageT;
import com.untangle.node.mail.papi.MIMEMessageTrickleT;
import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.MessageTransmissionTimeoutStrategy;
import com.untangle.node.mime.HeaderParseException;
import com.untangle.node.mime.InvalidHeaderDataException;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.mime.MIMEMessageHeaders;
import com.untangle.node.mime.MIMEPartParseException;
import com.untangle.node.mime.MIMEPolicy;
import com.untangle.node.token.AbstractTokenHandler;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenException;
import com.untangle.node.token.TokenResult;
import com.untangle.uvm.vnet.NodeTCPSession;

public abstract class PopStateMachine extends AbstractTokenHandler
{
    private final Logger logger = Logger.getLogger(getClass());

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
        MARKER,
        TRICKLE_START, /* state machine/node only state */
        TRICKLE_DATA, /* state machine/node only state */
        TRICKLE_MARKER, /* state machine/node only state */
        DONOTCARE_START, /* casing only state */
        DONOTCARE_DATA /* casing only state */
    };

    public enum ExceptionState {
        MESSAGE_COMPLETE,
        MESSAGE,
        MESSAGE_DATA,
        MESSAGE_MARKER
    };

    protected File zMsgFile;
    protected MIMEMessageT zMMessageT;
    protected MIMEMessage zMMessage; /* if set, header is null */
    protected MessageInfo zMsgInfo;
    protected long lTimeout;

    private ClientState clientState;
    private ServerState serverState;

    private FileChannel zWrChannel;
    private MIMEMessageHeaders zMMHeader; /* if set, message is null */

    private long lServerTS;

    // constructors -----------------------------------------------------------

    public PopStateMachine(NodeTCPSession session)
    {
        super(session);

        clientState = ClientState.COMMAND;
        serverState = ServerState.REPLY;

        zMsgFile = null;
        zMMessageT = null;
        zMMessage = null;
        zMsgInfo = null;

        zWrChannel = null;
        zMMHeader = null;

        updateServerTS();
    }

    // abstract methods -------------------------------------------------------

    protected abstract TokenResult scanMessage() throws TokenException;

    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        clientState = nextClientState(token);
        if (logger.isDebugEnabled()) {
            logger.debug("next state: " + clientState + ", " + this);
        }

        TokenResult zResult;

        switch (clientState) {
        case COMMAND:
            zResult = doPopCommand((PopCommand) token);
            break;

        case COMMAND_MORE:
            zResult = doPopCommandMore((PopCommandMore) token);
            break;

        default:
            ClientState tmpState = clientState;
            resetClient();
            throw new IllegalStateException("unexpected state: " + tmpState);
        }

        return zResult;
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        serverState = nextServerState(token);
        if (logger.isDebugEnabled()) {
            logger.debug("next state: " + serverState + ", " + this);
        }

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

        case MARKER:
            zResult = doMIMEMessageEnd((EndMarker) token);
            break;

        case TRICKLE_START:
            zResult = doMIMEMessageTrickle((MIMEMessageTrickleT) token);
            break;

        case TRICKLE_DATA:
            zResult = doMIMEMessageTrickleChunk((Chunk) token);
            break;

        case TRICKLE_MARKER:
            zResult = doMIMEMessageTrickleEnd((EndMarker) token);
            break;

        case DONOTCARE_START:
            zResult = doDoNotCare((DoNotCareT) token);
            break;

        case DONOTCARE_DATA:
            zResult = doDoNotCareChunk((DoNotCareChunkT) token);
            break;

        default:
            ServerState tmpState = serverState;
            resetServer();
            throw new IllegalStateException("unexpected state: " + tmpState);
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
            return trickleMessage(zMMessageT);
        }

        this.zMMessageT = zMMessageT;
        zMsgFile = zMMessageT.getFile();
        zMMHeader = zMMessageT.getMIMEMessageHeader();

        if (null == zMMHeader) {
            /* message has already been re-assembled and can be scanned */
            zMMessage = zMMessageT.getMIMEMessage();
            zMsgInfo = zMMessageT.getMessageInfo();

            TokenResult zResult;

            try {
                zResult = scanMessage();
            } catch (TokenException exn) {
                logger.warn("problem occurred during scan; scan result for message may have been discarded: " + exn);
                zResult = handleException(ExceptionState.MESSAGE_COMPLETE, zMMessageT);
                /* fall through */
            } catch (Throwable t) {
                logger.error("problem occurred during scan; scan result for message may have been discarded: " + t);
                zResult = handleException(ExceptionState.MESSAGE_COMPLETE, zMMessageT);
                /* fall through */
            }

            resetServer(); /* done so reset */
            return zResult;
        }
        /* else message needs to be re-assembled */

        try {
            zWrChannel = new FileOutputStream(zMsgFile, true).getChannel();
            return TokenResult.NONE; /* hold message reply for later */
        } catch (IOException exn) {
            /* cannot recover if byte unstuffed message file no longer exists */
            logger.error("byte unstuffed message file does not exist: ", exn);
            return handleException(ExceptionState.MESSAGE, zMMessageT);
        }
    }

    protected TokenResult doMIMEMessageChunk(Chunk zChunkT) throws TokenException
    {
        if (null != zMMessage) {
            /* cannot recover if message is missing */
            resetServer();
            throw new TokenException("message is not defined; cannot append chunk to message");
        }

        if (null != zMMessageT &&
            true == trickleNow(lTimeout, lServerTS)) {
            return trickleChunk(zChunkT);
        }

        return writeFile(zChunkT);
    }

    protected TokenResult doMIMEMessageEnd(EndMarker zEndMarkerT) throws TokenException
    {
        if (null != zMMessage) {
            resetServer();
            throw new TokenException("message is not defined; cannot end message");
        }

        if (null != zMMessageT &&
            true == trickleNow(lTimeout, lServerTS)) {
            return trickleMarker(zEndMarkerT);
        }

        try {
            zMMessage = new MIMEMessage(zMMessageT.getInputStream(), zMMessageT.getFileMIMESource(), new MIMEPolicy(), null);
        } catch (IOException exn) {
            logger.warn("cannot get FileMIMESource MIMEParsingInputStream: " + exn);
            return handleException(ExceptionState.MESSAGE_MARKER, zEndMarkerT);
        } catch (InvalidHeaderDataException exn) {
            logger.warn("cannot identify MIME message header: " + exn);
            return handleException(ExceptionState.MESSAGE_MARKER, zEndMarkerT);
        } catch (HeaderParseException exn) {
            logger.warn("cannot parse MIME message header: " + exn);
            return handleException(ExceptionState.MESSAGE_MARKER, zEndMarkerT);
        } catch (MIMEPartParseException exn) {
            logger.warn("cannot parse MIME message part(s): " + exn);
            return handleException(ExceptionState.MESSAGE_MARKER, zEndMarkerT);
        }

        zMMessageT.setMIMEMessage(zMMessage);
        zMMessageT.setMIMEMessageHeader(null); /* discard header */
        zMMHeader = null;

        zMsgInfo = zMMessageT.getMessageInfo();

        TokenResult zResult;
        try {
            zResult = scanMessage();
        } catch (TokenException exn) {
            logger.warn("problem occurred during scan; scan result for message may have been discarded: " + exn);
            zResult = handleException(ExceptionState.MESSAGE_COMPLETE, zMMessageT);
            /* fall through */
        } catch (Throwable t) {
            logger.error("problem occurred during scan; scan result for message may have been discarded: " + t);
            zResult = handleException(ExceptionState.MESSAGE_COMPLETE, zMMessageT);
            /* fall through */
        }

        resetServer(); /* done so reset */
        return zResult;
    }

    protected TokenResult doMIMEMessageTrickle(MIMEMessageTrickleT zMMTrickleT) throws TokenException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("trickling message (contd): " + zMMTrickleT.toString());
        }
        return new TokenResult(new Token[] { zMMTrickleT }, null);
    }

    protected TokenResult doMIMEMessageTrickleChunk(Chunk zChunkT) throws TokenException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("trickling chunk (contd): " + zChunkT.toString());
        }
        return new TokenResult(new Token[] { zChunkT }, null);
    }

    protected TokenResult doMIMEMessageTrickleEnd(EndMarker zEndMarkerT) throws TokenException
    {
        resetServer(); /* done so reset */

        if (logger.isDebugEnabled()) {
            logger.debug("trickling marker (contd): " + zEndMarkerT.toString());
        }
        return new TokenResult(new Token[] { zEndMarkerT }, null);
    }

    protected TokenResult doDoNotCare(DoNotCareT zDoNotCareT) throws TokenException
    {
        if (null == zMMessageT) {
            return new TokenResult(new Token[] { zDoNotCareT }, null);
        }
        /* else change MIMEMessageT,
         * that precedes DoNotCareT,
         * into MIMEMessageTrickleT and
         * push both on pipeline again
         *
         * - we should never enter this section
         */

        MIMEMessageTrickleT zMMTrickleT = new MIMEMessageTrickleT(zMMessageT);

        resetServer(); /* not really done but reset ... */
        serverState = ServerState.DONOTCARE_DATA; /* ... and change state */

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

    private TokenResult trickleMessage(MIMEMessageT zMMessageT)
    {
        MIMEMessageTrickleT zMMTrickleT = new MIMEMessageTrickleT(zMMessageT);
        if (logger.isDebugEnabled()) {
            logger.debug("trickling message: " + zMMessageT.toString() + ", " + zMMTrickleT.toString() + ", " + this);
        }
        serverState = ServerState.TRICKLE_START;
        if (logger.isDebugEnabled()) {
            logger.debug("next state: " + serverState + ", " + this);
        }
        return new TokenResult(new Token[] { zMMTrickleT }, null);
    }

    private TokenResult trickleChunk(Chunk zChunkT)
    {
        MIMEMessageTrickleT zMMTrickleT = new MIMEMessageTrickleT(zMMessageT);
        if (logger.isDebugEnabled()) {
            logger.debug("trickling message w/chunk: " + zMMessageT.toString() + ", " + zMMTrickleT.toString() + ", " + this);
        }
        zMMessageT = null;
        serverState = ServerState.TRICKLE_DATA;
        if (logger.isDebugEnabled()) {
            logger.debug("next state: " + serverState + ", " + this);
        }
        return new TokenResult(new Token[] { zMMTrickleT, zChunkT }, null);
    }

    private TokenResult trickleMarker(EndMarker zEndMarkerT)
    {
        MIMEMessageTrickleT zMMTrickleT = new MIMEMessageTrickleT(zMMessageT);
        if (logger.isDebugEnabled()) {
            logger.debug("trickling message w/marker: " + zMMessageT.toString() + ", " + zMMTrickleT.toString() + ", " + this);
        }
        zMMessageT = null;
        serverState = ServerState.TRICKLE_MARKER;
        if (logger.isDebugEnabled()) {
            logger.debug("next state: " + serverState + ", " + this);
        }
        return new TokenResult(new Token[] { zMMTrickleT, zEndMarkerT }, null);
    }

    private ClientState nextClientState(Token token) throws TokenException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("current state: " + clientState + ", " + token.toString() + ", " + this);
        }
        switch (clientState) {
        case COMMAND:
        case COMMAND_MORE:
            if (token instanceof PopCommand) {
                clientState = ClientState.COMMAND;
            } else if (token instanceof PopCommandMore) {
                clientState = ClientState.COMMAND_MORE;
            } else {
                ClientState tmpState = clientState;
                resetClient();
                throw new TokenException("cur: " + tmpState + ", next: bad token: " + token.toString());
            }

            return clientState;

        default:
            ClientState tmpState = clientState;
            resetClient();
            throw new IllegalStateException("bad state: " + tmpState);
        }
    }

    private ServerState nextServerState(Token token) throws TokenException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("current state: " + serverState + ", " + token.toString() + ", " + this);
        }
        switch (serverState) {
        case REPLY:
            if (token instanceof PopReply) {
                if (true == ((PopReply) token).isMsgData() ||
                    true == ((PopReply) token).isMsgHdrData()) {
                    serverState = ServerState.DATA_REPLY;
                } else {
                    // no change
                }
            } else if (token instanceof PopReplyMore) {
                serverState = ServerState.REPLY_MORE;
            } else if (token instanceof DoNotCareT) {
                serverState = ServerState.DONOTCARE_START;
            } else {
                ServerState tmpState = serverState;
                resetServer();
                throw new TokenException("cur: " + tmpState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case REPLY_MORE:
            if (token instanceof PopReply) {
                if (true == ((PopReply) token).isMsgData() ||
                    true == ((PopReply) token).isMsgHdrData()) {
                    serverState = ServerState.DATA_REPLY;
                } else {
                    serverState = ServerState.REPLY;
                }
            } else if (token instanceof PopReplyMore) {
                // no change
            } else {
                ServerState tmpState = serverState;
                resetServer();
                throw new TokenException("cur: " + tmpState + ", next: bad token: " + token.toString());
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
                ServerState tmpState = serverState;
                resetServer();
                throw new TokenException("cur: " + tmpState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case DATA_START:
            if (token instanceof Chunk) {
                serverState = ServerState.DATA;
            } else if (token instanceof EndMarker) {
                serverState = ServerState.MARKER;
            } else {
                ServerState tmpState = serverState;
                resetServer();
                throw new TokenException("cur: " + tmpState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case DATA:
            if (token instanceof Chunk) {
                // no change
            } else if (token instanceof EndMarker) {
                serverState = ServerState.MARKER;
            } else {
                ServerState tmpState = serverState;
                resetServer();
                throw new TokenException("cur: " + tmpState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case MARKER:
        case TRICKLE_MARKER:
            if (token instanceof PopReply) {
                if (true == ((PopReply) token).isMsgData() ||
                    true == ((PopReply) token).isMsgHdrData()) {
                    serverState = ServerState.DATA_REPLY;
                } else {
                    serverState = ServerState.REPLY;
                }
            } else {
                ServerState tmpState = serverState;
                resetServer();
                throw new TokenException("cur: " + tmpState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case TRICKLE_START:
            if (token instanceof PopReply) {
                /* leading state machine has already re-assembled message that
                 * we intended to trickle
                 * so we will not receive anything else to trickle;
                 * we must reset and restart
                 */
                resetServer();
                if (true == ((PopReply) token).isMsgData() ||
                    true == ((PopReply) token).isMsgHdrData()) {
                    serverState = ServerState.DATA_REPLY;
                } else {
                    serverState = ServerState.REPLY;
                }
            } else if (token instanceof PopReplyMore) {
                /* leading state machine has already re-assembled message that
                 * we intended to trickle
                 * so we will not receive anything else to trickle;
                 * we must reset and restart
                 */
                resetServer();
                serverState = ServerState.REPLY_MORE;
            } else if (token instanceof Chunk) {
                serverState = ServerState.TRICKLE_DATA;
            } else if (token instanceof EndMarker) {
                serverState = ServerState.TRICKLE_MARKER;
            } else if (token instanceof DoNotCareT) {
                serverState = ServerState.DONOTCARE_START;
            } else {
                ServerState tmpState = serverState;
                resetServer();
                throw new TokenException("cur: " + tmpState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case TRICKLE_DATA:
            if (token instanceof Chunk) {
                // no change
            } else if (token instanceof EndMarker) {
                serverState = ServerState.TRICKLE_MARKER;
            } else {
                ServerState tmpState = serverState;
                resetServer();
                throw new TokenException("cur: " + tmpState + ", next: bad token: " + token.toString());
            }

            return serverState;

        case DONOTCARE_START:
            serverState = ServerState.DONOTCARE_DATA;
            return serverState;

        case DONOTCARE_DATA:
            /* once we enter DONOTCARE stage, we cannot exit */
            return serverState;

        default:
            ServerState tmpState = serverState;
            resetServer();
            throw new IllegalStateException("bad state: " + tmpState);
        }
    }

    private TokenResult writeFile(Chunk zChunkT) throws TokenException
    {
        if (null != zWrChannel) {
            ByteBuffer zBuf = zChunkT.getBytes();

            try {
                for (; true == zBuf.hasRemaining(); ) {
                    zWrChannel.write(zBuf);
                }
            } catch (IOException exn) {
                logger.error("cannot write date to byte unstuffed message file: ", exn);
                return handleException(ExceptionState.MESSAGE_DATA, zChunkT);
            }
        }

        return TokenResult.NONE;
    }

    private FileChannel closeChannel(FileChannel zChannel)
    {
        if (null == zChannel) {
            return null;
        }

        try {
            zChannel.close();
        } catch (IOException exn) {
            logger.warn("cannot close message file: " + exn);
            /* fall through - don't stop */
        }

        return null;
    }

    private TokenResult handleException(ExceptionState zExceptState, Token zToken)
    {
        TokenResult zResult;

        switch (zExceptState) {
        case MESSAGE_COMPLETE:
            zResult = new TokenResult(new Token[] { zToken }, null);
            break;

        case MESSAGE:
            zResult = trickleMessage((MIMEMessageT) zToken);
            zMMessageT = null;
            break;

        case MESSAGE_DATA:
            zResult = trickleChunk((Chunk) zToken);
            break;

        case MESSAGE_MARKER:
            zResult = trickleMarker((EndMarker) zToken);
            break;

        default:
            logger.error("unsupported exception state: " + zExceptState);
            zResult = TokenResult.NONE;
            break;
        }

        return zResult;
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
        zWrChannel = closeChannel(zWrChannel);

        zMsgFile = null;
        zMMessageT = null;
        zMMessage = null;
        zMsgInfo = null;

        return;
    }
}
