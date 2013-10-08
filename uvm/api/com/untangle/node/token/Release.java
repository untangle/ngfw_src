/**
 * $Id: Release.java 34627 2013-05-03 18:30:42Z dmorris $
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

/**
 * This token means: drain queued data, send this data out, and
 * release the session.
 */
public class Release implements Token
{
    public static final Release EMPTY = new Release(ByteBuffer.allocate(0));

    private final ByteBuffer data;

    public Release(ByteBuffer data)
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

    public int getEstimatedSize()
    {
        return 0;
    }
}
