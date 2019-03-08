/**
 * $Id$
 */

package com.untangle.app.wan_balancer;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

/**
 * Event handler for the Wan Balancer application
 */
class EventHandler extends AbstractEventHandler
{
    private final WanBalancerApp app;

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Constructor
     * 
     * @param app
     *        The Wan Balancer application
     */
    EventHandler(WanBalancerApp app)
    {
        super(app);
        this.app = app;
    }

    /**
     * Handle new TCP sessions
     * 
     * @param sessionRequest
     *        The session
     */
    public void handleTCPNewSessionRequest(TCPNewSessionRequest sessionRequest)
    {
        handleNewSessionRequest(sessionRequest);
    }

    /**
     * Handle new UDP sessions
     * 
     * @param sessionRequest
     *        The session
     */
    public void handleUDPNewSessionRequest(UDPNewSessionRequest sessionRequest)
    {
        handleNewSessionRequest(sessionRequest);
    }

    /**
     * Handle new sessions
     * 
     * @param request
     *        The session request
     */
    private void handleNewSessionRequest(IPNewSessionRequest request)
    {
        int serverIntf = request.getServerIntf();

        /* If the server interface is not on an uplink this will do nothing */
        this.app.incrementDstInterfaceMetric(serverIntf);

        /* We don't care about this session anymore */
        request.release();
    }
}
