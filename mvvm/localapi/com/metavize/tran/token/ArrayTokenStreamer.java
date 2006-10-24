/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

public class ArrayTokenStreamer implements TokenStreamer
{
    private final Token[] toks;
    private final boolean closeWhenDone;
    int i = 0;

    public ArrayTokenStreamer(Token[] toks, boolean closeWhenDone)
    {
        this.toks = toks;
        this.closeWhenDone = closeWhenDone;
    }

    // TokenStreamer methods --------------------------------------------------

    public Token nextToken()
    {
        return i < toks.length ? toks[i] : null;
    }

    public boolean closeWhenDone()
    {
        return closeWhenDone;
    }
}
