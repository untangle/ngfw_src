/**
 * $Id: CaptureTrafficHandler.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture; // IMPL

import java.net.InetAddress;

import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.UvmContextFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

public class CaptureTrafficHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private CaptureNodeImpl node = null;

    public CaptureTrafficHandler(CaptureNodeImpl node)
    {
        super(node);
        this.node = node;
    }

///// TCP stuff --------------------------------------------------
    
    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        logger.debug("handleTCPNewSessionRequest()");
        super.handleTCPNewSessionRequest(event);
    }

    @Override
    public void handleTCPNewSession(TCPSessionEvent event)
    {
        NodeTCPSession session = event.session();
        String user = session.getClientAddr().getHostAddress();
        logger.debug("handleTCPNewSession " + user);

            // if the user is not in the active table we block the traffic
            if ((session.getServerPort() != 80) && (node.userTable.searchTable(user) == null))
            {
                logger.debug("Blocking TCP traffic for unauthenticated user " + user);
                session.resetClient();
                session.resetServer();
            }

        // release all sessions
        session.release();
        super.handleTCPNewSession(event);
    }

///// UDP stuff --------------------------------------------------

    @Override
    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
    {
        logger.debug("handleUDPNewSessionRequest()");
        super.handleUDPNewSessionRequest(event);
    }

    @Override
    public void handleUDPNewSession(UDPSessionEvent event)
    {
        NodeUDPSession session = event.session();
        String user = session.getClientAddr().getHostAddress();
        logger.debug("handleUDPNewSession " + user);
        
        // if the user is not in the active table we block the traffic
        if ((session.getServerPort() != 53) && (node.userTable.searchTable(user) == null))
        {
            logger.debug("Blocking UDP traffic for unauthenticated user " + user);
            session.expireClient();
            session.expireServer();
        }

        // release all sessions
        session.release();
        super.handleUDPNewSession(event);
    }

///// PRIVATE stuff ----------------------------------------------

}
