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

package com.untangle.uvm.vnet;

import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * A TCPSession is the most specific interface for VNet TCP sessions
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface TCPSession extends VnetSessionDesc, IPSession
{

    static final int TCP_MAX_CHUNK_SIZE = 65536;

    // Tells VNet to give a TCPClientReadableEvent with at most one line in the buffer.  (or
    // to the buffer limit if no end-of-line comes before that happens)
    // The end of line character(s) are left in the buffer.
    void clientLineBuffering(boolean oneLine);

    // Tells VNet to give a TCPServerReadableEvent with at most one line in the buffer.  (or
    // to the buffer limit if no end-of-line comes before that happens)
    // The end of line character(s) are left in the buffer.
    void serverLineBuffering(boolean oneLine);

    /**
     * <code>clientReadBufferSize</code> gives the size that a newly allocated read buffer for
     * reading from the client will be, in bytes.  This defaults from the node desc to
     * something like 8K.  Note that read buffers are only allocated when absolutely necessary.
     *
     * @return an <code>int</code> giving the capacity in bytes
     */
    int clientReadBufferSize();

    /**
     * Sets the <code>clientReadBufferSize</code> to the given number of bytes, which must be
     * >1, >=client_read_limit, <= MAX_BUFFER_SIZE
     *
     * @param numBytes an <code>int</code> value
     */
    void clientReadBufferSize(int numBytes);

    /**
     * <code>serverReadBufferSize</code> gives the size that a newly allocated read buffer for
     * reading from the server will be, in bytes.  This defaults from the node desc to
     * something like 8K.  Note that read buffers are only allocated when absolutely necessary.
     *
     * @return an <code>int</code> giving the capacity in bytes
     */
    int serverReadBufferSize();

    /**
     * Sets the <code>serverReadBufferSize</code> to the given number of bytes, which must be
     * >1, >=server_read_limit, <= MAX_BUFFER_SIZE
     *
     * @param numBytes an <code>int</code> value
     */
    void serverReadBufferSize(int numBytes);

    // To tell Vnet to give a TCPClientReadableEvent with at most this
    // many bytes in the buffer, adjust this. There is a system maximum.  Defaults
    // to the client read buffer size.
    int clientReadLimit();

    /**
     * <code>clientReadLimit</code> sets the maximum number of bytes that will be read from
     * the client for the next chunk.  Min is 1 byte, max and default is the read buffer size.
     *
     * @param numBytes an <code>int</code> value
     */
    void clientReadLimit(int numBytes);

    // To tell VNet to give a TCPServerReadableEvent with at most this
    // many bytes in the buffer, adjust this. There is a system maximum.  Defaults
    // to the server read buffer size.
    int serverReadLimit();

    /**
     * <code>serverReadLimit</code> sets the maximum number of bytes that will be read from
     * the server for the next chunk.  Min is 1 byte, max and default is the read buffer size.
     *
     * @param numBytes an <code>int</code> value
     */
    void serverReadLimit(int numBytes);

    /**
     * <code>shutdownClient</code> shuts down the output to the client.  Sends a FIN to the
     * client. (This is usually done in response to receiving a FIN at the server input.)
     * The FIN will be sent after sending any already-queued data.
     */
    void shutdownClient();

    /**
     * <code>shutdownServer</code> shuts down the output to the server.  Sends a FIN to the
     * client. (This is usually done in response to receiving a FIN at the client input.)
     * The FIN will be sent after sending any already-queued data.
     */
    void shutdownServer();

    /**
     * <code>shutdownClient</code> shuts down the output to the client.  Sends a FIN to the
     * client. (This is usually done in response to receiving a FIN at the server input.)
     *
     * @param force a <code>boolean</code> true if the FIN should go out ahead of any other data waiting to be sent, false if it should follow any such already-queued data.
     */
    void shutdownClient(boolean force);

    /**
     * <code>shutdownServer</code> shuts down the output to the server.  Sends a FIN to the
     * client. (This is usually done in response to receiving a FIN at the client input.)
     *
     * @param force a <code>boolean</code> true if the FIN should go out ahead of any other data waiting to be sent, false if it should follow any such already-queued data.
     */
    void shutdownServer(boolean force);

    /**
     * <code>resetClient</code> resets the client.  Sends a RST to the client.
     * (This is usually done in response to receiving a RST at the server input.)
     * Resets always go ahead of any already-queued data.
     *
     */
    void resetClient();

    /**
     * <code>resetServer</code> resets the server.  Sends a RST to the server.
     * (This is usually done in response to receiving a RST at the client input.)
     * Resets always go ahead of any already-queued data.
     *
     */
    void resetServer();

    /**
     * <code>beginClientStream</code> begins streaming to the client.  While streaming no
     * other chunk or writable events will be delivered until the stream is finished.  (This
     * happens when the streamer <code>nextChunk</code> function returns null.
     *
     * @param streamer a <code>TCPStreamer</code> value
     */
    void beginClientStream(TCPStreamer streamer);

    /**
     * <code>beginServerStream</code> begins streaming to the server.  While streaming no
     * other chunk or writable events will be delivered until the stream is finished.  (This
     * happens when the streamer <code>nextChunk</code> function returns null.
     *
     * @param streamer a <code>TCPStreamer</code> value
     */
    void beginServerStream(TCPStreamer streamer);

}
