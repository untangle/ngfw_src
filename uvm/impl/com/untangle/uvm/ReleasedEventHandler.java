/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.node.App;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * <code>ReleasedEventHandler</code> is a plain vanilla event handler used for released
 * sessions and whenever the node has no SessionEventHandler.  We just use everything
 * from AbstractEventHandler.
 */
class ReleasedEventHandler extends AbstractEventHandler
{
    ReleasedEventHandler(App node)
    {
        super(node);
    }
}
