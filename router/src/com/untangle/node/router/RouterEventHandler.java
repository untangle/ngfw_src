/**
 * $Id$
 */
package com.untangle.node.router;

import java.net.InetAddress;
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

public class RouterEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(RouterEventHandler.class);

    /* Router Node */
    private final RouterImpl node;

    /* Setup  */
    protected RouterEventHandler(RouterImpl node)
    {
        super(node);

        this.node = node;
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
        
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.TCP);
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
        
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.UDP);
    }

    private void handleNewSessionRequest(IPNewSessionRequest request, Protocol protocol)
    {
        InetAddress origClientAddr = request.getClientAddr();
        InetAddress newClientAddr = request.getNatFromHost();
        InetAddress origServerAddr = request.getServerAddr();
        InetAddress newServerAddr = request.getNatToHost();
        int origClientPort = request.getClientPort();
        int newClientPort  = request.getNatFromPort();
        int origServerPort = request.getServerPort();
        int newServerPort  = request.getNatToPort();

        if ( logger.isDebugEnabled()) {
            logger.debug( "pre-translation : " + origClientAddr + ":" + origClientPort +  " -> " + origServerAddr + ":" + origServerPort );
            logger.debug( "post-translation: " + newClientAddr + ":" + newClientPort +  " -> " + newServerAddr + ":" + newServerPort );
        }

        // if  we are a redirected session, we will already be registered with the
        // session manager. If so it will automatically delete the iptables rule that was used to
        // start this session
        if (node.getSessionManager().isSessionRedirect(request, protocol,node)){
            if ( logger.isDebugEnabled()) {
                logger.debug( "Found a redirected session");
            }
        }

        // if the kernel changed anything then we must be NATing.
        if ( !origClientAddr.equals(newClientAddr) ||
             !origServerAddr.equals(newServerAddr) ||
             (origClientPort != newClientPort) ||
             (origServerPort != newServerPort) ) {

            /* Update the session information so it matches what is in the NAT info */
            /* Here is where we have to insert the magic, just for TCP */
            request.setClientAddr( newClientAddr );
            request.setClientPort( newClientPort );
            request.setServerAddr( newServerAddr );
            request.setServerPort( newServerPort );

            if ( isFtp( request, protocol ) ) {
                if ( logger.isDebugEnabled()) {
                    logger.debug( "Ftp Session NATed, registering with the SessionManager");
                }
                node.getSessionManager().registerSession( request, protocol,
                                                          origClientAddr, origClientPort,
                                                          origServerAddr, origServerPort );
            } else {
                if ( logger.isDebugEnabled()) {
                    logger.debug( "non-Ftp Session NATed, not registering with the SessionManager");
                }
            }
        } else {
            if ( logger.isDebugEnabled()) {
                logger.debug( "Session Not NATed, so not registering with the SessionManager");
            }
        }

        //request.release();
    }

    public void handleTCPFinalized( TCPSessionEvent event )
    {
        cleanupSession( event.session() );
    }

    public void handleUDPFinalized( UDPSessionEvent event )
    {
        cleanupSession( event.session() );
    }

    private void cleanupSession( NodeSession session )
    {
        node.getSessionManager().releaseSession( session );
    }

    private boolean isFtp( IPNewSessionRequest request, Protocol protocol )
    {
        if ((protocol == Protocol.TCP) && (request.getServerPort() == 21)) {
            return true;
        }

        return false;
    }
}
