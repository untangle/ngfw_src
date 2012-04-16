/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import java.nio.ByteBuffer;

/**
 * IPDataResult is "mostly abstract" -- you probably want to be looking at
 * <code>TCPChunkResult</code> <code>UDPPacketResult</code>.
 */
public class IPDataResult
{
    // We don't use these publicly, instead use the static singletons above.
    private static final byte TYPE_NORMAL = 0;
    private static final byte TYPE_PASS_THROUGH = 1;

    public static final IPDataResult PASS_THROUGH = new IPDataResult(TYPE_PASS_THROUGH);
    public static final IPDataResult DO_NOT_PASS = new IPDataResult(null, null, null);
    public static final IPDataResult SEND_NOTHING = DO_NOT_PASS;

    // No access to this since we require using the immutable singletons for efficiency.
    private byte code = TYPE_NORMAL;

    // private IPStreamer clientStreamer = null;
    // private IPStreamer serverStreamer = null;

    private ByteBuffer[] bufsToServer = null;
    private ByteBuffer[] bufsToClient = null;
    private ByteBuffer readBuffer     = null;

    protected IPDataResult(byte code) 
    {
        this.code = code;
    }

    /**
     * This is the main constructor for IPDataResults.  The ByteBuffers accepted
     * here are assumed to be positioned and ready for writing/reading.  For the two
     * write buffers, that means:
     *  system will write out each buffer from position to limit.
     * for the read buffer, that means:
     *  system will read into the buffer starting from position to a max of limit
     *   XXX -- this conflicts/overrides
     * any of the buffers may be null, for the write buffers, that means:
     *  system will write nothing in that direction.
     * for the read buffer that means
     *  user is done with the bytes in the buffer and the buffer may be freed (if a
     *  system buffer, user-allocated buffers are not automatically freed.
     *
     * Note that the readBuffer is always null for UDP.
     *
     * @param bufsToClient a <code>ByteBuffer[]</code> giving bytes to be written to the client
     * @param bufsToServer a <code>ByteBuffer[]</code> giving bytes to be written to the server
     * @param readBuffer a <code>ByteBuffer</code> giving the buffer to be further read into.
     */
    protected IPDataResult(ByteBuffer[] bufsToClient, ByteBuffer[] bufsToServer, ByteBuffer readBuffer) 
    {
        this.bufsToClient = bufsToClient;
        this.bufsToServer = bufsToServer;
        this.readBuffer = readBuffer;
    }

    public ByteBuffer[] bufsToClient() 
    {
        return bufsToClient;
    }

    public ByteBuffer[] bufsToServer() 
    {
        return bufsToServer;
    }

    public ByteBuffer readBuffer() 
    {
        return readBuffer;
    }
    
    public byte getCode()
    {
    	return code;
    }


}

