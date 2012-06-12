/**
 * $Id$
 */
package com.untangle.uvm.engine;

import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.argon.ArgonUDPNewSessionRequest;

/**
 * Implementation class for UDP new session requests
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
class UDPNewSessionRequestImpl extends IPNewSessionRequestImpl implements UDPNewSessionRequest
{

    protected UDPNewSessionRequestImpl(Dispatcher disp, ArgonUDPNewSessionRequest argonRequest)
    {
        super(disp, argonRequest);
    }

}
