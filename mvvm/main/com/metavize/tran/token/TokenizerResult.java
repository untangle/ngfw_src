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

public class TokenizerResult
{
    private Token[] results;
    private ByteBuffer readBuffer;

    public TokenizerResult(Token[] results, ByteBuffer readBuffer)
    {
        this.results = results;
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
