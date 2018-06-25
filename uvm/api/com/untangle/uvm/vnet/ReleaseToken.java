/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

/**
 * This token means: drain queued data, send this data out, and
 * release the session.
 */
public class ReleaseToken implements Token
{
    public static final ByteBuffer EMPTY = ByteBuffer.allocate(0);

    /**
     * ReleaseToken constructor
     */
    public ReleaseToken() { }

    /**
     * getBytes - nothing in this token
     * @return ByteBuffer
     */
    public ByteBuffer getBytes()
    {
        return EMPTY;
    }
}
