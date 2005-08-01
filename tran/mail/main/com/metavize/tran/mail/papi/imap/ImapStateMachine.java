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

package com.metavize.tran.mail.papi.imap;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractTokenHandler;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import org.apache.log4j.Logger;

public abstract class ImapStateMachine extends AbstractTokenHandler
{
    private final Logger logger = Logger.getLogger(ImapStateMachine.class);

    // constructors -----------------------------------------------------------

    public ImapStateMachine(TCPSession session)
    {
        super(session);
    }

    // abstract methods -------------------------------------------------------


    // AbstractTokenHandler methods -------------------------------------------

    public TokenResult handleClientToken(Token token) throws TokenException
    {
        return new TokenResult(null, new Token[] { token });
    }

    public TokenResult handleServerToken(Token token) throws TokenException
    {
        return new TokenResult(new Token[] { token }, null);
    }
}
