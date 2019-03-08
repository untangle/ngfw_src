/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * Marks the end of a set of {@link Chunk}s.
 */
public class EndMarkerToken extends MetadataToken
{
    public static final EndMarkerToken MARKER = new EndMarkerToken();

    /**
     * EndMarkerToken - private constructor
     * Use MARKER singleton
     */
    private EndMarkerToken() { }
}
