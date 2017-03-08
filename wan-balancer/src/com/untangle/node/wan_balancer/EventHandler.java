/**
 * $Id$
 */
package com.untangle.node.wan_balancer;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

class EventHandler extends AbstractEventHandler
{
    private final WanBalancerApp node;

    private final Logger logger = Logger.getLogger(getClass());

    EventHandler(WanBalancerApp node)
    {
        super(node);
        this.node = node;
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest );
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest );
    }

    private void handleNewSessionRequest( IPNewSessionRequest request )
    {
        int serverIntf = request.getServerIntf();

        /* If the server interface is not on an uplink this will do nothing */
        this.node.incrementDstInterfaceMetric( serverIntf );

        /* We don't care about this session anymore */
        request.release();
    }
}
