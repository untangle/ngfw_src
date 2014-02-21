/**
 * $Id$
 */
package com.untangle.jnetcap;

public interface UDPPacketMailbox
{
    /**
     * Wait forever reading a packet from the packet mailbox.  Use with caution.
     */
    public UDPPacket read();
    
    /**
     * Timed wait to read a packet from the packet mailbox.</p>
     * @param timeout - Timeout in milliseconds
     */
    public UDPPacket read( int timeout );

    /* Retrieve the value of the C pointer */
    public long pointer();
}
