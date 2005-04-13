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

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractTokenHandler;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenResult;

public abstract class FtpStateMachine extends AbstractTokenHandler
{
    // constructors -----------------------------------------------------------

    protected FtpStateMachine(TCPSession session)
    {
        super(session);
    }

    // protected abstract -----------------------------------------------------

    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token)
    {
        return null; // XXX implement
    }

    public TokenResult handleServerToken(Token token)
    {
        return null; // XXX implement
    }

    private int nextClientState(Object o)
    {
        return -1; // XXX implement
    }

    private int nextServerState(Object o)
    {
        return -1; // XXX implement
    }
}
