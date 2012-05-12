/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.NodeUDPSession;

/**
 * Base class for all UDP live session events
 */
@SuppressWarnings("serial")
public class UDPSessionEvent extends IPSessionEvent
{
    
    public UDPSessionEvent(ArgonConnector argonConnector, NodeUDPSession session)
    {
        super(argonConnector, session);
    }

    public NodeUDPSession session()
    {
        return (NodeUDPSession)getSource();
    }
}
