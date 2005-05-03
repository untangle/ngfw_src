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

package com.metavize.tran.ftp;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractTokenHandler;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
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

        Pipeline p = MvvmContextFactory.context().pipelineFoundry()
            .getPipeline(session.id());
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
