/**
 * $Id$
 */
package com.untangle.uvm.engine;

import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.netcap.NetcapTCPNewSessionRequest;

/**
 * Implementation class for TCP new session requests
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
class TCPNewSessionRequestImpl extends IPNewSessionRequestImpl implements TCPNewSessionRequest
{
    protected TCPNewSessionRequestImpl(Dispatcher disp, NetcapTCPNewSessionRequest netcapRequest)
    {
        super(disp, netcapRequest);
    }

    public boolean acked()
    {
        return ((NetcapTCPNewSessionRequest)netcapRequest).acked();
    }

    public void rejectReturnRst()
    {
        ((NetcapTCPNewSessionRequest)netcapRequest).rejectReturnRst();
    }

}
