/**
 * $Id$
 */
package com.untangle.uvm.netcap;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.NodeTCPSession;

public class NetcapTCPNewSessionRequest extends NetcapIPNewSessionRequest
{
    final boolean acked;

    public NetcapTCPNewSessionRequest( SessionGlobalState sessionGlobalState, PipelineAgent agent, SessionEvent pe )
    {
        super( sessionGlobalState, agent, pe );

        /* Retrieve the value for acked */
        acked = sessionGlobalState.netcapTCPSession().acked();
    }

    public NetcapTCPNewSessionRequest( NodeTCPSession session, PipelineAgent agent, SessionEvent pe, SessionGlobalState sessionGlobalState)
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
