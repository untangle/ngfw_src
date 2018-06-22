/**
 * $Id$
 */

package com.untangle.uvm;

import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

/**
 * Class to manage TCP new session requests
 */
public class TCPNewSessionRequestImpl extends IPNewSessionRequestImpl implements TCPNewSessionRequest
{
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
    public TCPNewSessionRequestImpl(SessionGlobalState sessionGlobalState, PipelineConnectorImpl connector, SessionEvent pe)
    {
        super(sessionGlobalState, connector, pe);
    }

    /**
     * Constructor
     * 
     * @param prevRequest
     *        Previous session request
     * @param connector
     *        The pipeline connector
     * @param pe
     *        The session event
     * @param sessionGlobalState
     *        The session global state
     */
    public TCPNewSessionRequestImpl(TCPNewSessionRequestImpl prevRequest, PipelineConnectorImpl connector, SessionEvent pe, SessionGlobalState sessionGlobalState)
    {
        super(prevRequest, connector, pe, sessionGlobalState);
    }

    /**
     * <code>rejectReturnRst</code> rejects the new connection and sends a RST
     * to the client. Note that if <code>acked</code> is true, then a simple
     * close is done instead.
     */
    public void rejectReturnRst()
    {
        if (state != REQUESTED) {
            throw new IllegalStateException("Unable to reject session in state: " + state);
        }

        this.state = REJECTED;

        this.rejectCode = TCP_REJECT_RESET;
    }
}
