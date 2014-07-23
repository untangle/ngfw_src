/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

/**
 * A data chunk. 
 */
public class ChunkToken implements Token
{
    public static final ChunkToken EMPTY = new ChunkToken(ByteBuffer.allocate(0));

    private final ByteBuffer data;

    public ChunkToken(ByteBuffer data)
    {
        this.data = data;
    }

    /**
     * This method directly returns the internal ByteBuffer,
     * so changes to the returned ByteBuffer <b>will</b>
     * be seen by any downstream token handlers.
     */
    public ByteBuffer getData()
    {
        return data;
    }

    public int getSize()
    {
        return data.remaining();
    }

    /**
     * Returns a duplicate of the internal ByteBuffer, allowing
     * the caller to modify the returned ByteBuffer without concern
     * for any downstream token handlers.
     */
    public ByteBuffer getBytes()
    {
        return data.duplicate();
    }
}
