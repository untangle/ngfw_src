/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

/**
 * This token means: drain queued data, send this data out, and
 * release the session.
 */
public class ReleaseToken implements Token
{
    public static final ReleaseToken EMPTY = new ReleaseToken(ByteBuffer.allocate(0));

    private final ByteBuffer data;

    public ReleaseToken(ByteBuffer data)
    {
        this.data = data;
    }

    public ByteBuffer getData()
    {
        return data;
    }

    public ByteBuffer getBytes()
    {
        return data.duplicate();
    }
}
