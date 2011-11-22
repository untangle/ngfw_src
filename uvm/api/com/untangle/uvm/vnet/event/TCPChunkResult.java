/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.vnet.event;

import java.nio.ByteBuffer;


/**
 * TCP chunk result -- returned by node's event handler to indicate disposition of chunk
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
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
