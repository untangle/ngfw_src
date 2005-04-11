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

package com.metavize.tran.token;

import com.metavize.mvvm.tapi.TCPSession;

public abstract class AbstractTokenizer implements Tokenizer
{
    protected final TCPSession session;
    protected final boolean clientSide;

    // constructors -----------------------------------------------------------

    protected AbstractTokenizer(TCPSession session, boolean clientSide)
    {
        this.session = session;
        this.clientSide = clientSide;;
    }

    // session manipulation ---------------------------------------------------

    public void lineBuffering(boolean oneLine)
    {
        if (clientSide) {
            session.clientLineBuffering(oneLine);
        } else {
            session.serverLineBuffering(oneLine);
        }
    }

    public int readLimit()
    {
        if (clientSide) {
            return session.clientReadLimit();
        } else {
            return session.serverReadLimit();
        }
    }

    public void readLimit(int limit)
    {
        if (clientSide) {
            session.clientReadLimit(limit);
        } else {
            session.serverReadLimit(limit);
        }
    }

    public void scheduleTimer(long delay)
    {
        session.scheduleTimer(delay);
    }

    public void cancelTimer()
    {
        session.cancelTimer();
    }
}
