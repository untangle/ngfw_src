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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ParseResult
{
    private final List<Token> results;
    private final ByteBuffer readBuffer;

    // constructors -----------------------------------------------------------

    public ParseResult(List<Token> results, ByteBuffer readBuffer)
    {
        if (null == results) {
            this.results = new LinkedList<Token>();
        } else {
            this.results = results;
        }
        this.readBuffer = readBuffer;
    }

    public ParseResult(Token result, ByteBuffer readBuffer)
    {
        if (null == result) {
            this.results = new LinkedList<Token>();
        } else {
            this.results = Arrays.asList(result);
        }
        this.readBuffer = readBuffer;
    }

    public ParseResult(ByteBuffer readBuffer)
    {
        this.results = new LinkedList<Token>();
        this.readBuffer = readBuffer;
    }

    public ParseResult(Token result)
    {
        if (null == result) {
            this.results = new LinkedList<Token>();
        } else {
            this.results = Arrays.asList(result);
        }
        this.readBuffer = null;
    }

    public ParseResult(List<Token> tokens)
    {
        this.results = null == tokens ? new LinkedList<Token>() : tokens;
        this.readBuffer = null;
    }

    public ParseResult()
    {
        this.results = new LinkedList<Token>();
        this.readBuffer = null;
    }

    // accessors --------------------------------------------------------------

    public List<Token> getResults()
    {
        return results;
    }

    public ByteBuffer getReadBuffer()
    {
        return readBuffer;
    }
}
