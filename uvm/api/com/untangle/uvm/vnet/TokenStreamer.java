/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * Streams out a series of tokens.
 *
 */
public interface TokenStreamer
{
    Token nextToken();
    boolean closeWhenDone();
}
