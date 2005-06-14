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

public abstract class AbstractUnparser implements Unparser
{
    protected final TCPSession session;
    protected final boolean clientSide;

    protected AbstractUnparser(TCPSession session, boolean clientSide)
    {
        this.session = session;
        this.clientSide = clientSide;
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
}
