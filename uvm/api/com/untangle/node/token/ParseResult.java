/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Holds the result from parsing a chunk of data.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class ParseResult
{
    private final List<Token> results;
    private final ByteBuffer readBuffer;
    private final TokenStreamer tokenStreamer;

    // constructors -----------------------------------------------------------

    public ParseResult(List<Token> results, ByteBuffer readBuffer)
    {
        if (null == results) {
            this.results = new LinkedList<Token>();
        } else {
            this.results = results;
        }
        this.tokenStreamer = null;
        this.readBuffer = readBuffer;
    }

    public ParseResult(Token result, ByteBuffer readBuffer)
    {
        if (null == result) {
            this.results = new LinkedList<Token>();
        } else {
            this.results = Arrays.asList(result);
        }
        this.tokenStreamer = null;
        this.readBuffer = readBuffer;
    }

    public ParseResult(ByteBuffer readBuffer)
    {
        this.results = new LinkedList<Token>();
        this.tokenStreamer = null;
        this.readBuffer = readBuffer;
    }

    public ParseResult(Token result)
    {
        if (null == result) {
            this.results = new LinkedList<Token>();
        } else {
            this.results = Arrays.asList(result);
        }
        this.tokenStreamer = null;
        this.readBuffer = null;
    }

    public ParseResult(List<Token> tokens)
    {
        this.results = null == tokens ? new LinkedList<Token>() : tokens;
        this.tokenStreamer = null;
        this.readBuffer = null;
    }

    public ParseResult(TokenStreamer tokenStreamer, ByteBuffer readBuffer)
    {
        results = null;
        this.tokenStreamer = tokenStreamer;
        this.readBuffer = readBuffer;
    }

    public ParseResult()
    {
        this.results = new LinkedList<Token>();
        this.tokenStreamer = null;
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

    public TokenStreamer getTokenStreamer()
    {
        return tokenStreamer;
    }

    public boolean isStreamer()
    {
        return null != tokenStreamer;
    }
}
