/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.NodeSession;

/**
 * Base event class for all VNet session events
 *
 * For all session events, the source is the session.
 */
@SuppressWarnings("serial")
public abstract class SessionEvent extends ArgonConnectorEvent
{
    protected SessionEvent(ArgonConnector argonConnector, NodeSession session)
    {
        super(argonConnector, session);
    }
}
