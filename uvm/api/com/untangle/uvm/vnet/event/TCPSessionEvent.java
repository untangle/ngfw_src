/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Base class for all TCP live session events
 */
@SuppressWarnings("serial")
public class TCPSessionEvent extends IPSessionEvent
{
    public TCPSessionEvent(ArgonConnector argonConnector, NodeTCPSession session)
    {
        super(argonConnector, session);
    }

    public NodeTCPSession session()
    {
        return (NodeTCPSession)getSource();
    }
}
