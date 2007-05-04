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

public abstract class AbstractUnparser implements Unparser
{
    private final String idStr;

    protected final TCPSession session;
    protected final boolean clientSide;

    protected AbstractUnparser(TCPSession session, boolean clientSide)
    {
        this.session = session;
        this.clientSide = clientSide;

        String name = getClass().getName();

        this.idStr = name + "<" + (clientSide ? "CS" : "SS") + ":"
            + session.id() + ">";
    }

    // Unparser methods -------------------------------------------------------

    public UnparseResult releaseFlush()
    {
        return UnparseResult.NONE;
    }

    public void handleFinalized() {
        //Noop
    }

    // protected methods ------------------------------------------------------

    protected boolean isClientSide()
    {
        return clientSide;
    }

    protected TCPSession getSession()
    {
        return session;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return idStr;
    }
}
