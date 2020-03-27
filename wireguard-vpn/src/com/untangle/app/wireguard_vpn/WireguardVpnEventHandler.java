/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import org.apache.log4j.Logger;

/**
 * Event handler for Wireguard nVPN traffic
 * 
 * @author mahotz
 * 
 */
class WireguardVpnEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(WireguardVpnEventHandler.class);

    private final WireguardVpnApp app;

    /**
     * Constructor
     * 
     * @param app
     *        The application that created the handler
     */
    public WireguardVpnEventHandler(WireguardVpnApp app)
    {
        super(app);

        this.app = app;
    }

    /**
     * Handler for new TCP sessions
     * 
     * @param sessionRequest
     *        The TCP session request
     */
    public void handleTCPNewSessionRequest(TCPNewSessionRequest sessionRequest)

    {
        handleNewSessionRequest(sessionRequest);
    }

    /**
     * Handler for new UDP sessions
     * 
     * @param sessionRequest
     *        The UDP session request
     */
    public void handleUDPNewSessionRequest(UDPNewSessionRequest sessionRequest)

    {
        handleNewSessionRequest(sessionRequest);
    }

    /**
     * Handler for all sessions
     * 
     * @param request
     *        The IP session request
     */
    private void handleNewSessionRequest(IPNewSessionRequest request)
    {
        if (logger.isDebugEnabled()) logger.debug("New session: [" + request.id() + "]");

        logger.warn(request.getClientIntf() + " server=" + request.getServerIntf());
        if (request.getClientIntf() != InterfaceSettings.WIREGUARD_INTERFACE_ID && request.getServerIntf() != InterfaceSettings.WIREGUARD_INTERFACE_ID) {
            /* Nothing to do - not VPN traffic */
            request.release();
            return;
        } else if (request.getClientIntf() == InterfaceSettings.WIREGUARD_INTERFACE_ID && request.getServerIntf() == InterfaceSettings.WIREGUARD_INTERFACE_ID) {
            /* from the VPN to the VPN? just release it */
            request.release();
        } else if (request.getClientIntf() == InterfaceSettings.WIREGUARD_INTERFACE_ID) {
            /* Wireguard client going to another interface */
            app.incrementPassCount();
            request.release();
        } else {
            /* Local user trying to reach a Wireguard client */
            app.incrementPassCount();
            request.release();
        }
    }
}
