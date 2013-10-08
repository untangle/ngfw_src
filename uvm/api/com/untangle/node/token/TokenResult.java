/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/node/token/TokenResult.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.token;

/**
 * Result of a TokenHandler.
 *
 */
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
    /**
     * Test if this TokenResult has data (Streamer or tokens)
     * for the server (c2s)
     */
    public boolean hasDataForServer() {
        return c2sStreamer != null || (c2s!=null && c2s.length>0);
    }

    /**
     * Test if this TokenResult has data (Streamer or tokens)
     * for the client (s2c)
     */

    public boolean hasDataForClient() {
        return s2cStreamer != null || (s2c!=null && s2c.length>0);
    }

    // accessors --------------------------------------------------------------

    public Token[] s2cTokens()
    {
        return s2c;
    }

    public Token[] c2sTokens()
    {
        return c2s;
    }

    public TokenStreamer s2cStreamer()
    {
        return s2cStreamer;
    }

    public TokenStreamer c2sStreamer()
    {
        return c2sStreamer;
    }

    public boolean isStreamer()
    {
        return streamer;
    }
}
