/**
 * $Id$
 */

package com.untangle.uvm;

import com.untangle.jnetcap.NetcapUDPSession;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

/**
 * Class to manage UDP new session requests
 */
public class UDPNewSessionRequestImpl extends IPNewSessionRequestImpl implements UDPNewSessionRequest
{
    protected byte ttl;
    protected byte tos;

    /**
     * Constructor
     * 
     * @param sessionGlobalState
     *        The session global state
     * @param connector
     *        The pipeline connector
     * @param pe
     *        The session event
     */
    public UDPNewSessionRequestImpl(SessionGlobalState sessionGlobalState, PipelineConnectorImpl connector, SessionEvent pe)
    {
        super(sessionGlobalState, connector, pe);

        NetcapUDPSession netcapUDPSession = sessionGlobalState.netcapUDPSession();

        /* Grab the TTL, TOS from the udp session. */
        this.ttl = netcapUDPSession.ttl();
        this.tos = netcapUDPSession.tos();
    }

    /**
     * Constructor
     * 
     * @param prevRequest
     *        The previous request
     * @param connector
     *        The pipeline connector
     * @param pe
     *        The session event
     * @param sessionGlobalState
     *        Thhe session global state
     */
    public UDPNewSessionRequestImpl(UDPNewSessionRequestImpl prevRequest, PipelineConnectorImpl connector, SessionEvent pe, SessionGlobalState sessionGlobalState)
    {
        super(prevRequest, connector, pe, sessionGlobalState);

        /* Grab the TTL and TOS from the last request */
        this.ttl = prevRequest.ttl();
        this.tos = prevRequest.tos();
    }

    /**
     * Retrieve the TTL for a session, this only has an impact for the last
     * session in the chain when passing data crumbs (UDPPacketCrumbs have TTL
     * value inside of them)
     * 
     * @return The TTL
     */
    public byte ttl()
    {
        return ttl;
    }

    /**
     * Retrieve the TOS for a session, this only has an impact for the last
     * session in the chain when passing data crumbs (UDPPacketCrumbs have TOS
     * value inside of them).
     * 
     * @return The TOS
     */
    public byte tos()
    {
        return tos;
    }

    /**
     * Set the TTL for a session.</p>
     * 
     * @param value
     *        - new TTL value.
     */
    public void ttl(byte value)
    {
        ttl = value;
    }

    /**
     * Set the TOS for a session.</p>
     * 
     * @param value
     *        - new TOS value.
     */
    public void tos(byte value)
    {
        tos = value;
    }
}
