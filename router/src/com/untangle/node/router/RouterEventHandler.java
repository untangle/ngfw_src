/**
 * $Id: RouterEventHandler.java 36443 2013-11-19 23:32:09Z dmorris $
 */
package com.untangle.node.router;

import java.net.InetAddress;
import java.util.Random;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;
import org.apache.log4j.Logger;

class RouterEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(RouterEventHandler.class);

    /* Router Node */
    private final RouterImpl node;

    private int nextPort;
    
    /* Setup  */
    RouterEventHandler(RouterImpl node)
    {
        super(node);
        this.nextPort = (new Random().nextInt(20000)) + 10000;
        this.node = node;
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.TCP);
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
    {
        return;
    }

    private void handleNewSessionRequest(IPNewSessionRequest request, Protocol protocol)
    {
        InetAddress origClientAddr = request.getOrigClientAddr();
        InetAddress newClientAddr = request.getNewClientAddr();
        InetAddress origServerAddr = request.getOrigServerAddr();
        InetAddress newServerAddr = request.getNewServerAddr();
        int origClientPort = request.getOrigClientPort();
        int newClientPort  = request.getNewClientPort();
        int origServerPort = request.getOrigServerPort();
        int newServerPort  = request.getNewServerPort();

        if ( logger.isDebugEnabled()) {
            logger.debug( "pre-translation : " + origClientAddr + ":" + origClientPort +  " -> " + origServerAddr + ":" + origServerPort );
            logger.debug( "post-translation: " + newClientAddr + ":" + newClientPort +  " -> " + newServerAddr + ":" + newServerPort );
        }

        /**
         * The sole purpose of the router event handler is to rewrite the source port of
         * TCP connections
         *
         * The masquerading is handled by the kernel, however the kernel considers the
         * following two connections to be unique:
         *
         * 1.2.3.4:1234 -> 10.0.0.1:1234
         * 1.2.3.4:1234 -> 192.168.1.100:1234
         *
         * Because the kernel can differentiate the two even while both sessions
         * use the same port on 1.2.3.4.
         *
         * However, because we will be non-local binding we cant have both sessions
         * bind sockets to 1.2.3.4:1234
         *
         * As such, we must use or own port assignment scheme for TCP.
         * (UDP doesn't matter since we don't bind to sockets)
         */
        
        // if doing NAT, then rewrite the source port
        if ( !origClientAddr.equals(newClientAddr) ||
             !origServerAddr.equals(newServerAddr) ||
             (origClientPort != newClientPort) ||
             (origServerPort != newServerPort) ){

            if( protocol == Protocol.TCP && !origClientAddr.equals(newClientAddr)){
                int port = getNextPort();

                if ( logger.isDebugEnabled()) {
                    logger.debug( "Mangling server-side client port from " + origClientPort + " to " + port );
                }
                request.setNewClientPort( port );
            }
        }

        request.release();
    }


    private int getNextPort()
    {
        /**
         * FIXME
         *
         * This needs to be smarter.
         * It needs to either check the currently used list, or maintain a currently used list to avoid conflicts
         */
        
        if (nextPort > 50000)
            nextPort = 10000;

        return nextPort++;
    }
}
