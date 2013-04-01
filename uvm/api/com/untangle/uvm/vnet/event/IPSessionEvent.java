/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.ArgonConnector;

/**
 * Base class for all IP live session events
 */
@SuppressWarnings("serial")
public class IPSessionEvent extends SessionEvent
{

    public IPSessionEvent(ArgonConnector argonConnector, NodeSession session)
    {
        super(argonConnector, session);
    }

    public NodeSession ipsession()
    {
        return (NodeSession)getSource();
    }
}
