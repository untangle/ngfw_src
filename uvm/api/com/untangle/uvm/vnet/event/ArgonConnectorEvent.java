/**
 * $Id$
 */
package com.untangle.uvm.vnet.event;

import com.untangle.uvm.vnet.ArgonConnector;

/**
 * Top level abstract class for all VNet events
 */
@SuppressWarnings("serial")
public abstract class ArgonConnectorEvent extends java.util.EventObject
{
    private ArgonConnector argonConnector;

    protected ArgonConnectorEvent(ArgonConnector argonConnector, Object source)
    {
        super(source);
        this.argonConnector = argonConnector;
    }

    public ArgonConnector argonConnector()
    {
        return argonConnector;
    }
}
