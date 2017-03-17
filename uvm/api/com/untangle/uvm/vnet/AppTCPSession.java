/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.TCPStreamer;

/**
 * A TCPSession is the most specific interface for VNet TCP sessions
 */
public interface AppTCPSession extends AppSession
{
    static final byte CLOSED = 0;
    static final byte EXPIRED = 0;
    static final byte OPEN = 4;
    static final byte HALF_OPEN_INPUT = 5; /* for TCP */
    static final byte HALF_OPEN_OUTPUT = 6; /* for TCP */

    static final int TCP_MAX_CHUNK_SIZE = 65536;

    /**
     * Tells VNet to give a TCPClientReadableEvent with at most one line in the buffer.  (or
     * to the buffer limit if no end-of-line comes before that happens)
     * The end of line character(s) are left in the buffer.
     */
    void clientLineBuffering(boolean oneLine);

    /**
     * Tells VNet to give a TCPServerReadableEvent with at most one line in the buffer.  (or
     * to the buffer limit if no end-of-line comes before that happens)
     * The end of line character(s) are left in the buffer.
     */
    void serverLineBuffering(boolean oneLine);

    /**
     * <code>clientReadBufferSize</code> gives the size that a newly allocated read buffer for
     * reading from the client will be, in bytes.  This defaults from the app desc to
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
     * reading from the server will be, in bytes.  This defaults from the app desc to
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

    /** To tell Vnet to give a TCPClientReadableEvent with at most this
     * many bytes in the buffer, adjust this. There is a system maximum.  Defaults
     * to the client read buffer size.
     */
    long clientReadLimit();

    /**
     * <code>clientReadLimit</code> sets the maximum number of bytes that will be read from
     * the client for the next chunk.  Min is 1 byte, max and default is the read buffer size.
     *
     * @param numBytes an <code>long</code> value
     */
    void clientReadLimit(long numBytes);

    /** To tell VNet to give a TCPServerReadableEvent with at most this
     * many bytes in the buffer, adjust this. There is a system maximum.  Defaults
     * to the server read buffer size.
     */
    long serverReadLimit();

    /**
     * <code>serverReadLimit</code> sets the maximum number of bytes that will be read from
     * the server for the next chunk.  Min is 1 byte, max and default is the read buffer size.
     *
     * @param numBytes an <code>long</code> value
     */
    void serverReadLimit(long numBytes);

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
     * Sends a streamer to the specified side
     * The streamer will be streamed into the side until it is done.
     * Once it is done the it is remove from the write queue and the next
     * thing will be written (if there is one)
     */
    public void sendStreamer(int side, TCPStreamer streamer);

    /**
     * Sends a streamer to the client
     */
    public void sendStreamerToClient(TCPStreamer streamer);

    /**
     * Sends a streamer to the server
     */
    public void sendStreamerToServer(TCPStreamer streamer);
    
    /**
     * Delete temp files previously attached to this session, that might have 
     * been left over.
     */
    public void cleanupTempFiles();
    
    /**
     * Attach a temp file to this session in order to ensure cleanup on finalize 
     * or upon exception.
     * @param filePath
     */
    public void attachTempFile(String filePath);

    /**
     * Queue the provided buffers to be sent to the server
     */
    public void sendDataToServer( ByteBuffer[] bufs2send );

    /**
     * Queue the provided buffers to be sent to the client
     */
    public void sendDataToClient( ByteBuffer[] bufs2send );

    /**
     * Queue the provided buffers to be sent to the client or server
     * side can be AppSession.CLIENT or AppSession.SERVER
     */
    public void sendData( int side, ByteBuffer[] bufs2send );

    /**
     * Queue the provided buffer to be sent to the server
     */
    public void sendDataToServer( ByteBuffer buf2send );

    /**
     * Queue the provided buffer to be sent to the client
     */
    public void sendDataToClient( ByteBuffer buf2send );
    
    /**
     * Queue the provided buffer to be sent to the client or server
     * side can be AppSession.CLIENT or AppSession.SERVER
     */
    public void sendData( int side, ByteBuffer buf2send );

    /**
     * Set the client-side receive buffer to the provided buffer
     * When data is next received from the client it will put data
     * into the receive buffer starting at position up to limit
     */
    public void setClientBuffer( ByteBuffer buf );
    
    /**
     * Set the server-side receive buffer to the provided buffer
     * When data is next received from the server it will put data
     * into the receive buffer starting at position up to limit
     */
    public void setServerBuffer( ByteBuffer buf );

    /**
     * Set the client or server receive buffer to the provided buffer
     * side can be AppSession.CLIENT or AppSession.SERVER
     */
    public void setBuffer( int side, ByteBuffer buf );
}
