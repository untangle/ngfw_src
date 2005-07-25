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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.metavize.mvvm.tapi.TCPSession;
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

    public enum ClientState {
        COMMAND
    };

    public enum ServerState {
        REPLY,
        DATA_REPLY,
        DATA_START,
        DATA,
        END_MARKER
    };

    private ServerState serverState = ServerState.REPLY;

    private File zMsgFile;
    private FileChannel zMsgChannel;

    protected MIMEMessageHolderT zMMHolderT;
    protected MIMEMessage zMMessage; /* if set, header is null */
    protected MIMEMessageHeaders zMMHeader; /* if set, message is null */
    protected MessageInfo zMsgInfo;

    // constructors -----------------------------------------------------------

    public PopStateMachine(TCPSession session)
    {
        super(session);

        zMsgFile = null;
        zMsgChannel = null;

        zMMHolderT = null;
        zMMessage = null;
        zMMHeader = null;
        zMsgInfo = null;
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
        serverState = nextServerState(token);

        switch (serverState) {
        case REPLY:
        case DATA_REPLY:
            return doPopReply((PopReply) token);

        case DATA_START:
            return doMIMEMessageHolder((MIMEMessageHolderT) token);

        case DATA:
            return doMIMEMessageChunk((Chunk) token);

        case END_MARKER:
            return doMIMEMessageEnd((EndMarker) token);

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

    protected TokenResult doMIMEMessageHolder(MIMEMessageHolderT zMMHolderT) throws TokenException
    {
        this.zMMHolderT = zMMHolderT;

        zMMHeader = zMMHolderT.getMIMEMessageHeader();
        if (null == zMMHeader)
        {
            zMMessage = zMMHolderT.getMIMEMessage();
            zMsgInfo = zMMHolderT.getMessageInfo();
            return scanMessage();
        }

        try
        {
            zMsgFile = zMMHolderT.getFile();
            zMsgChannel = new FileOutputStream(zMsgFile).getChannel();
        }
        catch (IOException exn)
        {
            zMsgFile = null;
            zMsgChannel = null;
            logger.warn("cannot create message file: ", exn);
        }

        return TokenResult.NONE;
    }

    protected TokenResult doMIMEMessageChunk(Chunk zChunkT) throws TokenException
    {
        if (null != zMMessage)
        {
            return TokenResult.NONE; //XXXX ???
        }

        return writeFile(zChunkT);
    }

    protected TokenResult doMIMEMessageEnd(EndMarker zEndMarkerT) throws TokenException
    {
        if (null == zMMessage)
        {
            try
            {
                zMMessage = new MIMEMessage(zMMHolderT.getInputStream(), zMMHolderT.getFileMIMESource(), new MIMEPolicy(), null, zMMHeader);
            }
            catch (IOException exn)
            {
                logger.warn("cannot get FileMIMESource MIMEParsingInputStream: ", exn);
                return TokenResult.NONE; //XXXX ???
            }
            catch (InvalidHeaderDataException exn)
            {
                logger.warn("cannot create MIME message: ", exn);
                return TokenResult.NONE; //XXXX ???
            }
            catch (HeaderParseException exn)
            {
                logger.warn("cannot create MIME message: ", exn);
                return TokenResult.NONE; //XXXX ???
            }
            catch (MIMEPartParseException exn)
            {
                logger.warn("cannot create MIME message: ", exn);
                return TokenResult.NONE; //XXXX ???
            }

            zMMHolderT.setMIMEMessage(zMMessage);
            zMMHolderT.setMIMEMessageHeader(null); /* discard header */

            zMsgInfo = zMMHolderT.getMessageInfo();
        }

        closeMsgChannel();

        return scanMessage();
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
                throw new TokenException("bad token: " + token.getClass());
            }

            return serverState;

        case DATA_REPLY:
            serverState = ServerState.DATA_START;
            return serverState;

        case DATA_START:
            if (token instanceof EndMarker) {
                serverState = ServerState.END_MARKER;
            }
            else if (token instanceof Chunk) {
                serverState = ServerState.DATA;
            }

            return serverState;

        case DATA:
            if (token instanceof EndMarker) {
                serverState = ServerState.END_MARKER;
            }

            return serverState;

        case END_MARKER:
            if (token instanceof PopReply) {
                serverState = ServerState.REPLY;
            } else {
                throw new TokenException("bad token: " + token.getClass());
            }

            return serverState;

        default:
            throw new IllegalStateException("bad state: " + serverState);
        }
    }

    private TokenResult writeFile(Chunk zChunkT) throws TokenException
    {
        if (null != zMsgChannel)
        {
            try
            {
                for (ByteBuffer zBuf = zChunkT.getData();
                     true == zBuf.hasRemaining();
                     zMsgChannel.write(zBuf)) ;
            }
            catch (IOException exn)
            {
                closeMsgChannel();
                zMsgFile = null;
                throw new TokenException("cannot write date to message file: ", exn);
            }
        }

        return TokenResult.NONE;
    }

    private void closeMsgChannel()
    {
        try
        {
            zMsgChannel.close();
        }
        catch (IOException exn)
        {
            logger.warn("cannot close message file: ", exn);
        }
        finally
        {
            zMsgChannel = null;
        }

        return;
    }
}
