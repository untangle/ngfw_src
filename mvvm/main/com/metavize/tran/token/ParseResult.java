/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ParseResult.java,v 1.4 2005/01/14 00:41:20 amread Exp $
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;

public class ParseResult
{
    private Token[] results;
    private ByteBuffer readBuffer;

    public ParseResult(Token[] results, ByteBuffer readBuffer)
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
