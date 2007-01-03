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

package com.untangle.tran.token;

import com.untangle.mvvm.tapi.TCPSession;

public abstract class AbstractParser implements Parser
{
    private final String idStr;

    protected final TCPSession session;
    protected final boolean clientSide;

    // constructors -----------------------------------------------------------

    protected AbstractParser(TCPSession session, boolean clientSide)
    {
        this.session = session;
        this.clientSide = clientSide;

        String name = getClass().getName();

        this.idStr = name + "<" + (clientSide ? "CS" : "SS") + ":"
            + session.id() + ">";
    }

    /**
     * Get the underlying Session associated with this parser
     *
     * @return the session.
     */
    protected TCPSession getSession() {
      return session;
    }

    // Parser noops -----------------------------------------------------------

    public TokenStreamer endSession() { return null; }

    // session manipulation ---------------------------------------------------

    protected void lineBuffering(boolean oneLine)
    {
        if (clientSide) {
            session.clientLineBuffering(oneLine);
        } else {
            session.serverLineBuffering(oneLine);
        }
    }

    protected int readLimit()
    {
        if (clientSide) {
            return session.clientReadLimit();
        } else {
            return session.serverReadLimit();
        }
    }

    protected void readLimit(int limit)
    {
        if (clientSide) {
            session.clientReadLimit(limit);
        } else {
            session.serverReadLimit(limit);
        }
    }

    protected void scheduleTimer(long delay)
    {
        session.scheduleTimer(delay);
    }

    protected void cancelTimer()
    {
        session.cancelTimer();
    }

    protected boolean isClientSide() {
      return clientSide;
    }

    // no-ops methods ---------------------------------------------------------

    public void handleTimer() { }
    public void handleFinalized() { }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return idStr;
    }


}
