/**
 * $Id$
 */
package com.untangle.node.token;

/**
 * Streams out a series of tokens.
 *
 */
public interface TokenStreamer
{
    Token nextToken();
    boolean closeWhenDone();
}
