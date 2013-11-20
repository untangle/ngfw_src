/**
 * $Id$
 */
package com.untangle.node.token;


/**
 * Marks the end of a set of {@link Chunk}s.
 *
 */
public class EndMarker extends MetadataToken
{
    public static final EndMarker MARKER = new EndMarker();

    private EndMarker() { }
}
