/*
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.TCPSession;

/**
 * Base class for all TCP live session events
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class TCPSessionEvent extends IPSessionEvent
{

    public TCPSessionEvent(ArgonConnector argonConnector, TCPSession session)
    {
        super(argonConnector, session);
    }

    public TCPSession session()
    {
        return (TCPSession)getSource();
    }
}
