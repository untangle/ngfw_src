/**
 * $Id$
 */

package com.untangle.uvm;

import com.untangle.uvm.app.App;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * <code>ReleasedEventHandler</code> is a plain vanilla event handler used for
 * released sessions and whenever the app has no SessionEventHandler. We just
 * use everything from AbstractEventHandler.
 */
class ReleasedEventHandler extends AbstractEventHandler
{
    /**
     * Constructor
     * 
     * @param app
     *        The calling application
     */
    ReleasedEventHandler(App app)
    {
        super(app);
    }
}
