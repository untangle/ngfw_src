/**
 * $Id$
 */
package com.untangle.uvm.engine;

import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.argon.ArgonTCPNewSessionRequest;

/**
 * Implementation class for TCP new session requests
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
class TCPNewSessionRequestImpl extends IPNewSessionRequestImpl implements TCPNewSessionRequest
{
    protected TCPNewSessionRequestImpl(Dispatcher disp, ArgonTCPNewSessionRequest argonRequest)
    {
        super(disp, argonRequest);
    }

    public boolean acked()
    {
        return ((ArgonTCPNewSessionRequest)argonRequest).acked();
    }

    public void rejectReturnRst()
    {
        ((ArgonTCPNewSessionRequest)argonRequest).rejectReturnRst();
    }

}
