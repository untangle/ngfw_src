/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.NodeIPSession;
import com.untangle.uvm.vnet.ArgonConnector;

/**
 * Base class for all IP live session events
 */
@SuppressWarnings("serial")
public class IPSessionEvent extends SessionEvent
{

    public IPSessionEvent(ArgonConnector argonConnector, NodeIPSession session)
    {
        super(argonConnector, session);
    }

    public NodeIPSession ipsession()
    {
        return (NodeIPSession)getSource();
    }
}
