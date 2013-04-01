/**
 * $Id$
 */
package com.untangle.uvm.argon;

public class ArgonUDPSession extends ArgonIPSession
{
    protected final byte ttl;
    protected final byte tos;
    protected final byte options[];

    public ArgonUDPSession( ArgonUDPNewSessionRequest request )
    {
        super( request );

        ttl     = request.ttl();
        tos     = request.tos();
        options = request.options();
    }

    /**
     * Retrieve the TTL for a session, this only has an impact for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TTL value inside of them)
     */
    public byte ttl()
    {
        return ttl;
    }

    /**
     * Retrieve the TOS for a session, this only has an impact for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TOS value inside of them).
     */
    public byte tos()
    {
        return tos;
    }

    /**
     * Retrieve the options associated with the first UDP packet in the session.
     */
    public byte[] options()
    {
        return options;
    }

}
