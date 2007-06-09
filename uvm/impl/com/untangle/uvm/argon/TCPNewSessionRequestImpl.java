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

package com.untangle.uvm.argon;

import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.jnetcap.NetcapTCPSession;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;

class TCPNewSessionRequestImpl extends IPNewSessionRequestImpl implements TCPNewSessionRequest
{
    final boolean acked;

    public TCPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, ArgonAgent agent,
                                     byte originalServerIntf, PipelineEndpoints pe )
    {
        super( sessionGlobalState, agent, originalServerIntf, pe );

        /* Retrieve the value for acked */
        acked = sessionGlobalState.netcapTCPSession().acked();
    }

    public TCPNewSessionRequestImpl( TCPSession session, ArgonAgent agent, byte originalServerIntf, PipelineEndpoints pe )
    {
        super( session, agent, originalServerIntf, pe );

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
