/**
 * $Id$
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;

/**
 * A data chunk. 
 */
public class Chunk implements Token
{
    public static final Chunk EMPTY = new Chunk(ByteBuffer.allocate(0));

    private final ByteBuffer data;

    // constructors -----------------------------------------------------------

    public Chunk(ByteBuffer data)
    {
        this.data = data;
    }

    // accessors --------------------------------------------------------------

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

    // Token methods ----------------------------------------------------------

    /**
     * Returns a duplicate of the internal ByteBuffer, allowing
     * the caller to modify the returned ByteBuffer without concern
     * for any downstream token handlers.
     */
    public ByteBuffer getBytes()
    {
        return data.duplicate();
    }

    public int getEstimatedSize()
    {
        return data.remaining();
    }
}
