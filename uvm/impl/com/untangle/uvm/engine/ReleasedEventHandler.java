/**
 * $Id$
 */
package com.untangle.uvm.engine;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * <code>ReleasedEventHandler</code> is a plain vanilla event handler used for released
 * sessions and whenever the node has no SessionEventListener.  We just use everything
 * from AbstractEventHandler.
 */
class ReleasedEventHandler extends AbstractEventHandler
{
    ReleasedEventHandler(Node node)
    {
        super(node);
    }
}
