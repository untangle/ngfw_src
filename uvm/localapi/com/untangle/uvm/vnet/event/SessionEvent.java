/*
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.Session;

/**
 * Base event class for all VNet session events
 *
 * For all session events, the source is the session.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public abstract class SessionEvent extends ArgonConnectorEvent
{

    protected SessionEvent(ArgonConnector argonConnector, Session session)
    {
        super(argonConnector, session);
    }

}
