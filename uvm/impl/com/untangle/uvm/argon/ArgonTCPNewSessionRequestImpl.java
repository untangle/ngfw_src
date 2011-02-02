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

import com.untangle.uvm.node.PipelineEndpoints;

class ArgonTCPNewSessionRequestImpl extends ArgonIPNewSessionRequestImpl implements ArgonTCPNewSessionRequest
{
    final boolean acked;

    public ArgonTCPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, ArgonAgent agent, PipelineEndpoints pe )
    {
        super( sessionGlobalState, agent, pe );

        /* Retrieve the value for acked */
        acked = sessionGlobalState.netcapTCPSession().acked();
    }

    public ArgonTCPNewSessionRequestImpl( ArgonTCPSession session, ArgonAgent agent, PipelineEndpoints pe, SessionGlobalState sessionGlobalState)
    {
        super( session, agent, pe, sessionGlobalState);

        /* Retrieve the value for acked */
        acked = sessionGlobalState.netcapTCPSession().acked();
    }


    /**
     * <code>acked</code> returns true if the new session has already been ACKed to the client.
     * This occurs when the SYN shield has been activated.</p>
     *
     * If false, the SYN has not yet been ACKed.  In this case, the option to
     * <code>rejectReturnRst</code> is still available and if used will look to the client
     * as if no server was listening on that port.</p>
     *
     * @return True if the session was acked, false otherwise.
     */
    public boolean acked()
    {
        return acked;
    }
    
    /**
     * <code>rejectReturnRst</code> rejects the new connection and sends a RST to the client.
     * Note that if <code>acked</code> is true, then a simple close is done instead.
     */
    public void rejectReturnRst()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session in state: " + state  );
        }
        
        this.state = REJECTED;
        
        this.code = TCP_REJECT_RESET;
    }

}
