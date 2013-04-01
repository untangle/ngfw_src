/**
 * $Id$
 */
package com.untangle.uvm.netcap;

import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.NodeTCPSession;

public interface NewSessionEventListener
{
    /**
     * A new UDP session event event.  This function converts a request into a session.</p>
     *
     * @param request - A UDP NodeSession request.
     */
    public NodeUDPSession newSession( NetcapUDPNewSessionRequest request );

    /**
     * A new TCP session event event.  This function converts a request into a session.</p>
     *
     * @param request - A TCP NodeSession request.
     */
    public NodeTCPSession newSession( NetcapTCPNewSessionRequest request );
}
