/**
 * $Id$
 */
package com.untangle.node.token;

/**
 * Token streamer that streams an array of tokens.
 *
 */
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
