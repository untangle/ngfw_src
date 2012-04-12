/**
 * $Id$
 */
package com.untangle.uvm.node;

public interface UDPSessionDesc extends IPSessionDesc
{
    /**
     * Retrieve the TTL for a session, this only has an "impact" for
     * the last session in the chain when passing data crumbs
     * (UDPPacketCrumbs have TTL value inside of them).
     */
    byte ttl();

    /**
     * Retrieve the TOS for a session, this only has an "impact" for
     * the last session in the chain when passing data crumbs
     * (UDPPacketCrumbs have TOS value inside of them).
     */
    byte tos();

    /**
     * Retrieve the options associated with the first UDP packet in
     * the session.
     */
    byte[] options();

}
