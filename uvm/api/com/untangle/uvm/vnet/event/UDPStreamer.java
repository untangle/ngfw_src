/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

/**
 * Streamer for UDP sessions (not yet implemented)
 */
public interface UDPStreamer extends IPStreamer
{
    /**
     * <code>nextPacket</code> should return a ByteBuffer containing the next packet to
     * be sent.  (Bytes are sent from the buffer's position to its limit).  Packets must
     * be less than the maximum packet size appropriate for the session.  Returns null for
     * "no more packets to send".
     *
     * @return a <code>ByteBuffer</code> giving the bytes of the next chunk to send.  Null when done.
     */
    // Not even needed yet:
    // UDPPacketCrumb nextPacket();
}
