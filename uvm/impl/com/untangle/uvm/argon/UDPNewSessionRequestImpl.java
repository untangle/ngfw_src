/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.argon;

import com.untangle.mvvm.tran.PipelineEndpoints;
import com.untangle.jnetcap.NetcapUDPSession;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;

class UDPNewSessionRequestImpl extends IPNewSessionRequestImpl implements UDPNewSessionRequest
{
    protected byte ttl;
    protected byte tos;
    protected byte[] options;
    protected int icmpId;
    private final boolean isPing;

    public UDPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, ArgonAgent agent,
                                     byte originalServerIntf, PipelineEndpoints pe )
    {
        super( sessionGlobalState, agent, originalServerIntf, pe );

        NetcapUDPSession netcapUDPSession = sessionGlobalState.netcapUDPSession();

        /* Grab the TTL, TOS, isPing and ICMP Identifier from the udp session. */
        this.ttl    = netcapUDPSession.ttl();
        this.tos    = netcapUDPSession.tos();
        this.icmpId = ( netcapUDPSession.isIcmpSession()) ? netcapUDPSession.icmpClientId() : 0;

        /* XXX ICMP Hack, PING is presently the only ICMP session. */
        this.isPing = netcapUDPSession.isIcmpSession();
    }
    
    public UDPNewSessionRequestImpl( UDPSession session, ArgonAgent agent, byte originalServerIntf, PipelineEndpoints pe )
    {
        super( session, agent, originalServerIntf, pe );

        /* Grab the TTL and TOS from the last request */
        this.ttl    = session.ttl();
        this.tos    = session.tos();
        this.icmpId = session.icmpId();
        this.isPing = session.isPing();
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
     * Returns true if this is a Ping session
     */
    public boolean isPing()
    {
        return this.isPing;
    }

    /**
     * Retrieve the ICMP associated with the session
     */
    public int icmpId()
    {
        return icmpId;
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
    
    /**
     * Set the ICMP id for this session.</p>
     * @param value - new icmp id value, -1 to not modify.
     */
    public void icmpId( int value )
    {
        this.icmpId = value;
    }
}
