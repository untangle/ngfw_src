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

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * A UDPSession is the most specific interface for VNet UDP sessions
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface UDPSession extends UDPSessionDesc, IPSession
{
    static final int UDP_MAX_MESG_SIZE = 65536;

    /**
     * <code>clientMaxPacketSize</code> gives the size that a newly allocated read buffer for
     * reading from the client will be, in bytes.  This defaults from the node desc to
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
     * reading from the server will be, in bytes.  This defaults from the node desc to
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

    void sendClientError(byte icmpType, byte icmpCode, ByteBuffer icmpData, InetAddress source, IPPacketHeader header);

    void sendServerError(byte icmpType, byte icmpCode, ByteBuffer icmpData, InetAddress source, IPPacketHeader header);

}
