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
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;

// XXX hook the data and ctl into same state machine
public abstract class FtpStateMachine extends AbstractTokenHandler
{
    private final Fitting clientFitting;
    private final Fitting serverFitting;

    // constructors -----------------------------------------------------------

    protected FtpStateMachine(TCPSession session)
    {
        super(session);

        Pipeline p = MvvmContextFactory.context().pipelineFoundry()
            .getPipeline(session.id());
        clientFitting = p.getClientFitting(session.mPipe());
        serverFitting = p.getServerFitting(session.mPipe());
    }

    // protected abstract -----------------------------------------------------

    protected abstract TokenResult doClientData(Chunk c) throws TokenException;
    protected abstract void doClientDataEnd() throws TokenException;

    protected abstract TokenResult doServerData(Chunk c) throws TokenException;
    protected abstract void doServerDataEnd() throws TokenException;

    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        if (Fitting.FTP_CTL_TOKENS == clientFitting) {
            return new TokenResult(null, new Token[] { token });
        } else if (Fitting.FTP_DATA_TOKENS == clientFitting) {
            return doClientData((Chunk)token);
        } else {
            throw new IllegalStateException("bad fitting: " + clientFitting);
        }
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        if (Fitting.FTP_CTL_TOKENS == serverFitting) {
            return new TokenResult(new Token[] { token }, null);
        } else if (Fitting.FTP_DATA_TOKENS == serverFitting) {
            return doServerData((Chunk)token);
        } else {
            throw new IllegalStateException("bad fitting: " + serverFitting);
        }
    }

    @Override
    public void handleClientFin() throws TokenException
    {
        if (Fitting.FTP_DATA_TOKENS == clientFitting) {
            doClientDataEnd();
        }
    }

    @Override
    public void handleServerFin() throws TokenException
    {
        if (Fitting.FTP_DATA_TOKENS == serverFitting) {
            doServerDataEnd();
        }
    }
}
