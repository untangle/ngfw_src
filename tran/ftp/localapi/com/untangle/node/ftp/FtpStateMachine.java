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

package com.untangle.node.ftp;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.tapi.Fitting;
import com.untangle.uvm.tapi.Pipeline;
import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.token.AbstractTokenHandler;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenException;
import com.untangle.node.token.TokenResult;
import org.apache.log4j.Logger;

// XXX hook the data and ctl into same state machine
public abstract class FtpStateMachine extends AbstractTokenHandler
{
    private final Fitting clientFitting;
    private final Fitting serverFitting;

    private final Logger logger = Logger.getLogger(FtpStateMachine.class);

    // constructors -----------------------------------------------------------

    protected FtpStateMachine(TCPSession session)
    {
        super(session);

        Pipeline p = getPipeline();
        clientFitting = p.getClientFitting(session.mPipe());
        serverFitting = p.getServerFitting(session.mPipe());
    }

    // protected methods ------------------------------------------------------

    protected TokenResult doCommand(FtpCommand command) throws TokenException
    {
        return new TokenResult(null, new Token[] { command });
    }

    protected TokenResult doReply(FtpReply reply) throws TokenException
    {
        return new TokenResult(new Token[] { reply }, null);
    }

    protected TokenResult doClientData(Chunk c) throws TokenException
    {
        return new TokenResult(null, new Token[] { c });
    }

    protected void doClientDataEnd() throws TokenException { }

    protected TokenResult doServerData(Chunk c) throws TokenException
    {
        return new TokenResult(new Token[] { c }, null);
    }

    protected void doServerDataEnd() throws TokenException { }

    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        if (Fitting.FTP_CTL_TOKENS == clientFitting) {
            return doCommand((FtpCommand)token);
        } else if (Fitting.FTP_DATA_TOKENS == clientFitting) {
            if (token instanceof EndMarker) {
                return new TokenResult(null, new Token[] { EndMarker.MARKER });
            } else if (token instanceof Chunk) {
                return doClientData((Chunk)token);
            } else {
                throw new TokenException("bad token: " + token);
            }
        } else {
            throw new IllegalStateException("bad fitting: " + clientFitting);
        }
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        if (Fitting.FTP_CTL_TOKENS == serverFitting) {
            return doReply((FtpReply)token);
        } else if (Fitting.FTP_DATA_TOKENS == serverFitting) {
            if (token instanceof EndMarker) {
                return new TokenResult(new Token[] { EndMarker.MARKER }, null);
            } else if (token instanceof Chunk) {
                return doServerData((Chunk)token);
            } else {
                throw new TokenException("bad token: " + token);
            }
        } else {
            throw new IllegalStateException("bad fitting: " + serverFitting);
        }
    }

    @Override
    public void handleClientFin() throws TokenException
    {
        doClientDataEnd();
    }

    @Override
    public void handleServerFin() throws TokenException
    {
        doServerDataEnd();
    }
}
