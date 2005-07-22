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

public class TokenResult
{
    public static final TokenResult NONE;

    private static final Token[] TOKEN_PROTO;

    static {
        TOKEN_PROTO = new Token[0];
        NONE = new TokenResult();
    }

    // XXX probably better realized as subclasses
    private final boolean streamer;

    // XXX convert to list?
    // XXX this is fugly,
    private final Token[] s2c;
    private final Token[] c2s;

    private final TokenStreamer s2cStreamer;
    private final TokenStreamer c2sStreamer;

    // constructors -----------------------------------------------------------

    // XXX ugly!

    private TokenResult()
    {
        this.s2c = TOKEN_PROTO;
        this.c2s = TOKEN_PROTO;
        this.s2cStreamer = null;
        this.c2sStreamer = null;
        streamer = false;
    }

    public TokenResult(Token[] s2c, Token[] c2s)
    {
        this.s2c = null == s2c ? TOKEN_PROTO : s2c;
        this.c2s = null == c2s ? TOKEN_PROTO : c2s;
        this.s2cStreamer = null;
        this.c2sStreamer = null;
        this.streamer = false;
    }

    public TokenResult(TokenStreamer s2cStreamer, TokenStreamer c2sStreamer)
    {
        this.s2cStreamer = s2cStreamer;
        this.c2sStreamer = c2sStreamer;
        this.s2c = null;
        this.c2s = null;
        this.streamer = true;
    }

    // accessors --------------------------------------------------------------

    Token[] s2cTokens()
    {
        return s2c;
    }

    Token[] c2sTokens()
    {
        return c2s;
    }

    TokenStreamer s2cStreamer()
    {
        return s2cStreamer;
    }

    TokenStreamer c2sStreamer()
    {
        return c2sStreamer;
    }

    boolean isStreamer()
    {
        return streamer;
    }
}
