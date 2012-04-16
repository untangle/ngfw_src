/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

/**
 * New TCP session request event
 */
@SuppressWarnings("serial")
public class TCPNewSessionRequestEvent extends ArgonConnectorEvent
{
    
    public TCPNewSessionRequestEvent(ArgonConnector argonConnector, TCPNewSessionRequest sessionRequest)
    {
        super(argonConnector, sessionRequest);
    }

    public TCPNewSessionRequest sessionRequest()
    {
        return (TCPNewSessionRequest)getSource();
    }

}
