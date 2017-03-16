/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

public class TCPNewSessionRequestImpl extends IPNewSessionRequestImpl implements TCPNewSessionRequest
{
    public TCPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, PipelineConnectorImpl connector, SessionEvent pe )
    {
        super( sessionGlobalState, connector, pe );
    }

    public TCPNewSessionRequestImpl( TCPNewSessionRequestImpl prevRequest, PipelineConnectorImpl connector, SessionEvent pe, SessionGlobalState sessionGlobalState)
    {
        super( prevRequest, connector, pe, sessionGlobalState);
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
        
        this.rejectCode = TCP_REJECT_RESET;
    }

}
