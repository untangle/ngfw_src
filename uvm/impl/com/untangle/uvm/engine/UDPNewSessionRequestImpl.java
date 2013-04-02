/**
 * $Id: UDPNewSessionRequestImpl.java -1   $
 */
package com.untangle.uvm.engine;

import com.untangle.jnetcap.NetcapUDPSession;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.netcap.SessionGlobalState;

public class UDPNewSessionRequestImpl extends IPNewSessionRequestImpl implements UDPNewSessionRequest
{
    protected byte ttl;
    protected byte tos;
    protected byte[] options;

    public UDPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, PipelineConnectorImpl connector, SessionEvent pe )
    {
        super( sessionGlobalState, connector, pe );

        NetcapUDPSession netcapUDPSession = sessionGlobalState.netcapUDPSession();

        /* Grab the TTL, TOS from the udp session. */
        this.ttl    = netcapUDPSession.ttl();
        this.tos    = netcapUDPSession.tos();
    }
    
    public UDPNewSessionRequestImpl( NodeUDPSession session, PipelineConnectorImpl connector, SessionEvent pe, SessionGlobalState sessionGlobalState)
    {
        super( session, connector, pe, sessionGlobalState );

        /* Grab the TTL and TOS from the last request */
        this.ttl    = ((NodeUDPSessionImpl)session).ttl();
        this.tos    = ((NodeUDPSessionImpl)session).tos();
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
    
    /**
     * Set the TTL for a session.</p>
     * @param value - new TTL value.
     */
    public void ttl( byte value )
    {
        ttl = value;
    }
    
    /**
     * Set the TOS for a session.</p>
     * @param value - new TOS value.
     */
    public void tos( byte value )
    {
        tos = value;
    }

    /**
     * Set the options for this session.</p>
     * @param value - The new options.
     */
    public void options( byte[] value )
    {
        options = value;
    }
    
}
