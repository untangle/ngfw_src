/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UnparseSession.java,v 1.5 2005/01/29 00:20:22 amread Exp $
 */

package com.metavize.tran.token;

import com.metavize.mvvm.tapi.TCPSession;

public class UnparseSession
{
    private final TCPSession session;
    private final boolean first;

    UnparseSession(TCPSession session, boolean first)
    {
        this.session = session;
        this.first = first;
    }

    public String toString()
    {
        return session.id() + " (" + (first ? "first" : "second") + ") ";
    }
}
