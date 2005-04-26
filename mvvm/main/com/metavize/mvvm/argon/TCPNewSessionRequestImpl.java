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

import com.metavize.jnetcap.NetcapTCPSession;
import com.metavize.jvector.IncomingSocketQueue;
import com.metavize.jvector.OutgoingSocketQueue;

class TCPNewSessionRequestImpl extends IPNewSessionRequestImpl implements TCPNewSessionRequest
{
    final boolean acked;

    public TCPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, ArgonAgent agent )
    {
        super( sessionGlobalState, agent );

        /* Retrieve the value for acked */
        acked = sessionGlobalState.netcapTCPSession().acked();
    }

    public TCPNewSessionRequestImpl( TCPSession session, ArgonAgent agent )
    {
        super( session, agent);

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
        /* XXX Need some implementation */
    {
    }

}
