/**
 * $Id$
 */
package com.untangle.jnetcap;

/**
 * UDPPacketMailbox is an interface for a mailbox to receive UDP packets
 */
public interface UDPPacketMailbox
{
    /**
     * Wait forever reading a packet from the packet mailbox.  Use with caution.
     * @return the UDPPacket
     */
    public UDPPacket read();
    
    /**
     * Timed wait to read a packet from the packet mailbox.</p>
     * @param timeout - Timeout in milliseconds
     * @return the UDPPacket
     */
    public UDPPacket read( int timeout );

    /**
     * Retrieve the value of the C pointer
     * @return - the pointer
     */
    public long pointer();
}
