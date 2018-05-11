/**
 * $Id$
 */
package com.untangle.jnetcap;

public interface UDPPacket 
{
    /**
     * Retrieve the destination info about the packet.
     * @return the UDPAttributes
     */
    public UDPAttributes attributes();
    
    /**
     * Retrieve the data from the packet, this will allocate a buffer of the proper size automatically.
     * @return the data
     */
    public byte[] data();

    /**
     * Retrieve the data from a packet and place the results into the preallocated <code>buffer</code>.
     * @param buffer - Location where the data should be stored.
     * @return size of the data returned.
     */
    public int getData( byte[] buffer );

    /**
     * Send out this packet 
     */
    public void send();

    /**
     * Raze this packet 
     */
    public void raze();
}
