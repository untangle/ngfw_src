/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.UDPSession;

/**
 * Base class for all UDP live session events
 */
@SuppressWarnings("serial")
public class UDPSessionEvent extends IPSessionEvent
{
    
    public UDPSessionEvent(ArgonConnector argonConnector, UDPSession session)
    {
        super(argonConnector, session);
    }

    public UDPSession session()
    {
        return (UDPSession)getSource();
    }
}
