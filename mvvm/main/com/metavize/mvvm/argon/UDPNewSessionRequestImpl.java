/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.argon;

import com.metavize.mvvm.tran.PipelineEndpoints;
import com.metavize.jnetcap.NetcapUDPSession;
import com.metavize.jvector.IncomingSocketQueue;
import com.metavize.jvector.OutgoingSocketQueue;

class UDPNewSessionRequestImpl extends IPNewSessionRequestImpl implements UDPNewSessionRequest
{
    protected byte ttl;
    protected byte tos;
    protected byte[] options;
    protected int icmpId;

    public UDPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, ArgonAgent agent,
                                     byte originalServerIntf, PipelineEndpoints pe )
    {
        super( sessionGlobalState, agent, originalServerIntf, pe );
        
        /* Grab the TTL, TOS and ICMP Identifier from the udp session */
        this.ttl    = sessionGlobalState.netcapUDPSession().ttl();
        this.tos    = sessionGlobalState.netcapUDPSession().tos();
        this.icmpId = sessionGlobalState.netcapUDPSession().icmpClientId();
    }
    
    public UDPNewSessionRequestImpl( UDPSession session, ArgonAgent agent, byte originalServerIntf, PipelineEndpoints pe )
    {
        super( session, agent, originalServerIntf, pe );

        /* Grab the TTL and TOS from the last request */
        this.ttl    = session.ttl();
        this.tos    = session.tos();
        this.icmpId = session.icmpId();
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
