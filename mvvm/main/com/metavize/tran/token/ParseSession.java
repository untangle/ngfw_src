/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ParseSession.java,v 1.7 2005/01/29 00:20:22 amread Exp $
 */

package com.metavize.tran.token;

import com.metavize.mvvm.tapi.TCPSession;
import org.apache.log4j.Logger;

public class ParseSession
{
    private final Logger logger = Logger.getLogger(ParseSession.class);

    private final TCPSession session;
    private final boolean first;

    ParseSession(TCPSession session, boolean first)
    {
        this.session = session;
        this.first = first;
    }

    public TCPSession session()
    {
        return session;
    }

    public boolean isFirst()
    {
        return first;
    }

    public void lineBuffering(boolean oneLine)
    {
        if (first) {
            logger.debug("client line buffering: " + oneLine);
            session.clientLineBuffering(oneLine);
        } else {
            logger.debug("server line buffering:" + oneLine);
            session.serverLineBuffering(oneLine);
        }
    }

    public int readLimit()
    {
        if (first) {
            return session.clientReadLimit();
        } else {
            return session.serverReadLimit();
        }
    }

    public void readLimit(int limit)
    {
        if (first) {
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

    public String toString()
    {
        return session.id() + " (" + (first ? "first" : "second") + ") ";
    }
}
