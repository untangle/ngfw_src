/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPNewSessionRequestImpl.java,v 1.7 2005/01/31 03:18:39 rbscott Exp $
 */

package com.metavize.mvvm.argon;

import com.metavize.jnetcap.NetcapUDPSession;

import com.metavize.jvector.IncomingSocketQueue;
import com.metavize.jvector.OutgoingSocketQueue;

class UDPNewSessionRequestImpl extends IPNewSessionRequestImpl implements UDPNewSessionRequest
{
    protected byte ttl;
    protected byte tos;
    protected byte[] options;

    public UDPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, ArgonAgent agent )
    {
        super( sessionGlobalState, agent );
        
        /* Grab the TTL and TOS from the udp session */
        ttl = sessionGlobalState.netcapUDPSession().ttl();
        tos = sessionGlobalState.netcapUDPSession().tos();
    }

    public UDPNewSessionRequestImpl( UDPSession session, ArgonAgent agent )
    {
        super( session.sessionGlobalState(), agent );

        /* Grab the TTL and TOS from the last request */
        ttl = session.ttl();
        tos = session.tos();
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
