/**
 * $Id$
 */
package com.untangle.node.token;

/**
 * Casings are responsible for breaking byte-streams into tokens and
 * vice versa. They come in pairs with one side near the server and
 * the other near the client. Traffic on the inside is passed
 * as tokens, and on the outside as a raw byte stream.
 */

public interface Casing
{
    public abstract Parser parser();
    public abstract Unparser unparser();
}
