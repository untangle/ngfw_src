/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * A TCPSession is the most specific interface for VNet TCP sessions
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
