/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FtpStateMachine.java,v 1.10 2005/01/28 10:27:31 amread Exp $
 */

package com.metavize.tran.ftp;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractTokenHandler;
import com.metavize.tran.token.TokenEvent;
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

    public TokenResult handleClientToken(TokenEvent e)
    {
        return null; // XXX implement
    }

    public TokenResult handleServerToken(TokenEvent e)
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
