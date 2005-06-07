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

package com.metavize.tran.spam;

import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.MessageFile;
import com.metavize.tran.mail.MimeBoundary;
import com.metavize.tran.mail.Rfc822Header;
import com.metavize.tran.mail.SmtpCommand;
import com.metavize.tran.mail.SmtpReply;
import com.metavize.tran.mail.SmtpStateMachine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import org.apache.log4j.Logger;

class SpamSmtpHandler extends SmtpStateMachine
{
    private final Logger logger = Logger.getLogger(SpamSmtpHandler.class);

    private static final int CUTOFF = 1 << 20;
    private static final float SPAM_SCORE = 5;

    private final List<Token> msgQueue = new LinkedList<Token>();

    private int size = 0;
    boolean passthrough = false;

    private Rfc822Header header;

    // constructors -----------------------------------------------------------

    SpamSmtpHandler(TCPSession session)
    {
        super(session);
    }

    // SmtpStateMachine methods -----------------------------------------------

    protected TokenResult doSmtpCommand(SmtpCommand cmd)
        throws TokenException
    {
        return new TokenResult(null, new Token[] { cmd });
    }

    protected TokenResult doMessageHeader(Rfc822Header header)
        throws TokenException
    {
        if (null == this.header) {
            this.header = header;
        }

        return queueOrPassThrough(header);
    }

    protected TokenResult doBodyChunk(Chunk chunk) throws TokenException
    {
        return queueOrPassThrough(chunk);
    }

    protected TokenResult doPreamble(Chunk chunk)
        throws TokenException
    {
        return queueOrPassThrough(chunk);
    }

    protected TokenResult doBoundary(MimeBoundary boundary, boolean end)
        throws TokenException
    {
        return queueOrPassThrough(boundary);
    }

    protected TokenResult doMultipartHeader(Rfc822Header header)
        throws TokenException
    {
        return queueOrPassThrough(header);
    }

    protected TokenResult doMultipartBody(Chunk chunk)
        throws TokenException
    {
        return queueOrPassThrough(chunk);
    }

    protected TokenResult doEpilog(Chunk chunk)
        throws TokenException
    {
        return queueOrPassThrough(chunk);
    }

    protected TokenResult doMessageFile(MessageFile messageFile)
        throws TokenException
    {
        if (passthrough) {
            assert 0 == msgQueue.size();
            // to avoid keeping track of the message stack, i'll use this
            // to reset state for now
            size = 0;
            header = null;
            passthrough = false;
            return new TokenResult(null, new Token[] { messageFile });
        } else {
            logger.debug("scanning message");
            SpamReport sr = SpamAssassin.ASSASSIN.scan(messageFile.getFile(),
                                                       SPAM_SCORE);

            msgQueue.add(messageFile);

            if (logger.isDebugEnabled()) {
                logger.debug("Spam Report: " + sr);
            }

            logger.debug("rewriting header");
            sr.rewriteHeader(header);
            Token[] msgs = new Token[msgQueue.size()];
            msgs = msgQueue.toArray(msgs);
            msgQueue.clear();
            return new TokenResult(null, msgs);
        }
    }

    protected TokenResult doMessageEnd(EndMarker endMarker)
        throws TokenException
    {
        return new TokenResult(null, new Token[] { endMarker });
    }

    protected TokenResult doSmtpReply(SmtpReply reply)
        throws TokenException
    {
        return new TokenResult(new Token[] { reply }, null);
    }

    // private methods --------------------------------------------------------

    private TokenResult queueOrPassThrough(Token token)
    {
        if (!passthrough && token instanceof Chunk) {
            size += ((Chunk)token).getData().remaining();
            if (CUTOFF <= size) {
                passthrough = true;
            }
        }

        if (passthrough) {
            logger.debug("in passthrough mode");
            if (msgQueue.size() > 0) {
                msgQueue.add(token);
                Token[] toks = new Token[msgQueue.size()];
                msgQueue.toArray(toks);
                msgQueue.clear();
                return new TokenResult(null, toks);
            } else {
                return new TokenResult(null, new Token[] { token });
            }
        } else {
            logger.debug("still queueing");
            msgQueue.add(token);
            return TokenResult.NONE;
        }
    }
}
