/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.tapi.event;

import java.nio.ByteBuffer;


public class TCPChunkResult extends IPDataResult
{
    private static final byte TYPE_READ_MORE_NO_WRITE = 2;

    public static final TCPChunkResult READ_MORE_NO_WRITE = new TCPChunkResult(TYPE_READ_MORE_NO_WRITE);

    private TCPChunkResult(byte code)
    {
        super(code);
    }

    /**
     * Creates a <code>TCPChunkResult</code> to denote that the engine should
     * send the contents of the given buffers in a single gathering write (as
     * efficiently as possible). One or more of these buffers may be the
     * read-buffer from the event, with the <b>current</b> position and limit used. It may
     * also be some other buffer; again the buffer's current position and limit
     * are used.
     *
     * @param chunksToClient a <code>ByteBuffer[]</code> containing the buffers to be gathered and written to the client, with each data chunk starting at its buffer's position, extending to its buffer's limit.  Null means write nothing to the client.
     * @param chunksToServer a <code>ByteBuffer[]</code> containing the buffers to be gathered and written to the server, with each data chunk starting at its buffer's position, extending to its buffer's limit  Null means write nothing to the server.
     * @param readBuffer a <code>ByteBuffer</code> giving the buffer to be used for further reading from the source.  new bytes are read beginning at position, extending to at most the limit.  Null means the system will throw away the read-buffer and returns the session to a bufferless, lowest-wastage state.
     */
    public TCPChunkResult(ByteBuffer[] chunksToClient,
                          ByteBuffer[] chunksToServer,
                          ByteBuffer readBuffer) {
        super(chunksToClient, chunksToServer, readBuffer);
    }

    /**
     * Creates a <code>TCPChunkResult</code> to denote that the engine should
     * send an entire stream to one side.
     * Note that only one of clientStreamer or serverStreamer may be supplied, the other must be null.
     *
     * @param clientStreamer a <code>TCPStreamer</code> giving the stream to be chunked to the client.  If non-null, serverStreamer must be null.
     * @param serverStreamer a <code>TCPStreamer</code> giving the stream to be chunked to the server.  If non-null, clientStreamer must be null.

    /*
     * Streaming now handled in IPSession.
     *
    public TCPChunkResult(TCPStreamer clientStreamer, TCPStreamer serverStreamer) {
        super(clientStreamer, serverStreamer);
    }
    */

    public ByteBuffer[] chunksToClient() {
        return bufsToClient();
    }

    public ByteBuffer[] chunksToServer() {
        return bufsToServer();
    }
}
