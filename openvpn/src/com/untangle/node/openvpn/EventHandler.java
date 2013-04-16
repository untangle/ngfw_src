/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import org.apache.log4j.Logger;

class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger( EventHandler.class );

    private final OpenVpnNodeImpl node;

    public EventHandler( OpenVpnNodeImpl node )
    {
        super(node);

        this.node = node;
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequestEvent event )
        
    {
        handleNewSessionRequest( event.sessionRequest());
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequestEvent event )
        
    {
        handleNewSessionRequest( event.sessionRequest());
    }

    private void handleNewSessionRequest( IPNewSessionRequest request )
    {
        if ( logger.isDebugEnabled()) logger.debug( "New session: [" + request.id() + "]" );

        if ( request.getClientIntf() != IntfConstants.OPENVPN_INTF && request.getServerIntf() != IntfConstants.OPENVPN_INTF ) {
            /* Nothing to do - not VPN traffic*/
            request.release();
            return;
        }
        else if ( request.getClientIntf() == IntfConstants.OPENVPN_INTF && request.getServerIntf() == IntfConstants.OPENVPN_INTF ) {
            /* from the VPN to the VPN? just release it */
            request.release();
        }
        else if ( request.getClientIntf() == IntfConstants.OPENVPN_INTF ) {
            /* OPENVPN client going to another interface */
            node.incrementPassCount();
            request.release();
        }
        else {
            /* Local user trying to reach a OPENVPN client */
            node.incrementPassCount();
            request.release();
        }
    }
}
