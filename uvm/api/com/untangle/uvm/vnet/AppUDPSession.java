/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.nio.ByteBuffer;

/**
 * A UDPSession is the most specific interface for VNet UDP sessions
 */
public interface AppUDPSession extends AppSession
{
    static final int UDP_MAX_MESG_SIZE = 65536;

    /**
     * <code>clientMaxPacketSize</code> gives the size that a newly allocated read buffer for
     * reading from the client will be, in bytes.  This defaults from the app desc to
     * something like 8K.  Note that read buffers are only allocated when absolutely necessary.
     *
     * @return an <code>int</code> giving the capacity in bytes
     */
    int clientMaxPacketSize();

    /**
     * Sets the <code>clientMaxPacketSize</code> to the given number of bytes, which must be
     * >1, >=client_read_limit, <= MAX_BUFFER_SIZE
     *
     * @param numBytes an <code>int</code> value
     */
    void clientMaxPacketSize(int numBytes);

    /**
     * <code>serverMaxPacketSize</code> gives the size that a newly allocated read buffer for
     * reading from the server will be, in bytes.  This defaults from the app desc to
     * something like 8K.  Note that read buffers are only allocated when absolutely necessary.
     *
     * @return an <code>int</code> giving the capacity in bytes
     */
    int serverMaxPacketSize();

    /**
     * Sets the <code>serverMaxPacketSize</code> to the given number of bytes, which must be
     * >1, >=server_read_limit, <= MAX_BUFFER_SIZE
     *
     * @param numBytes an <code>int</code> value
     */
    void serverMaxPacketSize(int numBytes);

    /**
     * <code>expireClient</code> expires the client size of the UDP session.
     * (Propogats the expire to the client. If the client is at the end, the
     * kernel is told to expire the session on that end. (This is usually done
     * in response to receiving an expire at the server input.) The expire
     * will be sent immediately, before any already-queued packets.
     */
    void expireClient();

    /**
     * <code>expireServer</code> expires the server size of the UDP session.
     * (Propogats the expire to the server. If the server is at the end, the
     * kernel is told to expire the session on that end. (This is usually done
     * in response to receiving an expire at the client input.) The expire
     * will be sent immediately, before any already-queued packets.
     */
    void expireServer();

    void sendClientPacket(ByteBuffer packet, IPPacketHeader header);

    void sendServerPacket(ByteBuffer packet, IPPacketHeader header);

}
