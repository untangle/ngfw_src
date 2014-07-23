/**
 * $Id$
 */
package com.untangle.node.token;

/**
 * Marks the end of a set of {@link Chunk}s.
 */
public class EndMarkerToken extends MetadataToken
{
    public static final EndMarkerToken MARKER = new EndMarkerToken();

    private EndMarkerToken() { }
}
