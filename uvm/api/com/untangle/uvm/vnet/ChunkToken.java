/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

/**
 * A data chunk. 
 */
public class ChunkToken implements Token
{
    public static final ChunkToken EMPTY = new ChunkToken(ByteBuffer.allocate(0));

    private final ByteBuffer data;

    /**
     * ChunkToken constructor
     * @param data
     */
    public ChunkToken(ByteBuffer data)
    {
        this.data = data;
    }

    /**
     * This method directly returns the internal ByteBuffer,
     * so changes to the returned ByteBuffer <b>will</b>
     * be seen by any downstream token handlers.
     * @return ByteBuffer
     */
    public ByteBuffer getData()
    {
        return data;
    }

    /**
     * getSize - return the size of the data encapsulated in this ChunkToken
     * @return int
     */
    public int getSize()
    {
        return data.remaining();
    }

    /**
     * Returns a duplicate of the internal ByteBuffer, allowing
     * the caller to modify the returned ByteBuffer without concern
     * for any downstream token handlers.
     * @return ByteBuffer
     */
    public ByteBuffer getBytes()
    {
        return data.duplicate();
    }
}
