/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.argon;

import com.untangle.jnetcap.NetcapUDPSession;
import com.untangle.uvm.node.PipelineEndpoints;

class ArgonUDPNewSessionRequestImpl extends ArgonIPNewSessionRequestImpl implements ArgonUDPNewSessionRequest
{
    protected byte ttl;
    protected byte tos;
    protected byte[] options;

    public ArgonUDPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, ArgonAgent agent, PipelineEndpoints pe )
    {
        super( sessionGlobalState, agent, pe );

        NetcapUDPSession netcapUDPSession = sessionGlobalState.netcapUDPSession();

        /* Grab the TTL, TOS from the udp session. */
        this.ttl    = netcapUDPSession.ttl();
        this.tos    = netcapUDPSession.tos();
    }
    
    public ArgonUDPNewSessionRequestImpl( ArgonUDPSession session, ArgonAgent agent, PipelineEndpoints pe, SessionGlobalState sessionGlobalState)
    {
        super( session, agent, pe, sessionGlobalState );

        /* Grab the TTL and TOS from the last request */
        this.ttl    = session.ttl();
        this.tos    = session.tos();
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
