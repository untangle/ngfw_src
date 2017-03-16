/**
 * $Id$
 */
package com.untangle.app.openvpn;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.app.IPMatcher;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
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
