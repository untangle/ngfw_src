/**
 * $Id$
 */
package com.untangle.uvm.engine;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.ArgonConnector;

/**
 * Binds a <code>Fitting</code> to a <code>ArgonConnector</code>.
 */
class ArgonConnectorFitting
{
    final ArgonConnector argonConnector;
    final Fitting fitting;
    final ArgonConnector end;

    ArgonConnectorFitting(ArgonConnector argonConnector, Fitting fitting)
    {
        this.argonConnector = argonConnector;
        this.fitting = fitting;
        this.end = null;
    }

    ArgonConnectorFitting(ArgonConnector argonConnector, Fitting fitting, ArgonConnector end)
    {
        this.argonConnector = argonConnector;
        this.fitting = fitting;
        this.end = end;
    }

    // Object methods ---------------------------------------------------------

    @Override
    public String toString()
    {
        return argonConnector.toString();
    }
}
