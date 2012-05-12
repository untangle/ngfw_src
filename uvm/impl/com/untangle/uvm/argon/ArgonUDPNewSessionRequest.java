/**
 * $Id$
 */
package com.untangle.uvm.argon;

public interface ArgonUDPNewSessionRequest extends ArgonIPNewSessionRequest, ArgonIPSessionDesc
{
    /**
     * Retrieve the TTL for a session, this only has an impact for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TTL value inside of them)
     */
    byte ttl();

    /**
     * Retrieve the TOS for a session, this only has an impact for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TOS value inside of them).
     */
    byte tos();

    /**
     * Retrieve the options associated with the first UDP packet in the session.
     */
    byte[] options();

    /**
     * Set the TTL for a session.</p>
     * @param value - new TTL value.
     */
    void ttl( byte value );
    
    /**
     * Set the TOS for a session.</p>
     * @param value - new TOS value.
     */
    void tos( byte value );

    /**
     * Set the options for this session.</p>
     * @param value - The new options.
     */
    void options( byte[] value );

}
