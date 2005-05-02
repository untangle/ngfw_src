/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;

public class ParseResult
{
    private static final Token[] TOKEN_PROTO = new Token[0];
    private final Token[] results;
    private final ByteBuffer readBuffer;

    public ParseResult(Token[] results, ByteBuffer readBuffer)
    {
        this.results = null == results ? TOKEN_PROTO : results;
        this.readBuffer = readBuffer;
    }

    public Token[] results()
    {
        return results;
    }

    public ByteBuffer readBuffer()
    {
        return readBuffer;
    }
}
