/*
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.UDPSession;

/**
 * Base class for all UDP live session events
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
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
