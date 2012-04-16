/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

/**
 * New UDP session request event
 */
@SuppressWarnings("serial")
public class UDPNewSessionRequestEvent extends ArgonConnectorEvent
{
    public UDPNewSessionRequestEvent(ArgonConnector argonConnector, UDPNewSessionRequest sessionRequest)
    {
        super(argonConnector, sessionRequest);
    }

    public UDPNewSessionRequest sessionRequest()
    {
        return (UDPNewSessionRequest)getSource();
    }
}
