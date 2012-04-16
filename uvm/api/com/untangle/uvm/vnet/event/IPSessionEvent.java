/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.IPSession;
import com.untangle.uvm.vnet.ArgonConnector;

/**
 * Base class for all IP live session events
 */
@SuppressWarnings("serial")
public class IPSessionEvent extends SessionEvent
{

    public IPSessionEvent(ArgonConnector argonConnector, IPSession session)
    {
        super(argonConnector, session);
    }

    public IPSession ipsession()
    {
        return (IPSession)getSource();
    }
}
