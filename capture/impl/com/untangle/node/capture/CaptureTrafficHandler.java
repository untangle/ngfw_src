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
        super.handleTCPNewSessionRequest(event);
    }

    @Override
    public void handleTCPNewSession(TCPSessionEvent event)
    {
        NodeTCPSession session = event.session();
        String address = session.getClientAddr().getHostAddress();
        CaptureUserEntry user = node.captureUserTable.searchByAddress(address);

        // if we have an authenticated user allow traffic and release session
        if (user != null)
        {
            logger.debug("Allowing TCP traffic for authenticated user " + address);
            user.updateActivityTimer();
            session.release();
            return;
        }

        // not authenicated so allow all web traffic so the http
        // casing can create the redirect to the captive page
        if (session.getServerPort() == 80)
        {
            logger.debug("Allowing HTTP traffic for unauthenticated user " + address);
            session.release();
            return;
        }

        // user not authenticated and not http traffic so block
        logger.debug("Blocking TCP traffic for unauthenticated user " + address);
        session.resetClient();
        session.resetServer();
        session.release();
    }

///// UDP stuff --------------------------------------------------

    @Override
    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
    {
        super.handleUDPNewSessionRequest(event);
    }

    @Override
    public void handleUDPNewSession(UDPSessionEvent event)
    {
        NodeUDPSession session = event.session();
        String address = session.getClientAddr().getHostAddress();
        CaptureUserEntry user = node.captureUserTable.searchByAddress(address);

        // if we have an authenticated user allow traffic and release session
        if (user != null)
        {
            logger.debug("Allowing UDP traffic for authenticated user " + address);
            user.updateActivityTimer();
            session.release();
            return;
        }

        // not authenticated so we allow UDP traffic so the initial DNS lookup
        // will succeed allowing for the redirect to the captive page
        if (session.getServerPort() == 53)
        {
            logger.debug("Allowing DNS traffic for unauthenticated user " + address);
            session.release();
            return;
        }

        // user not authenticated and not dns traffic so block
        logger.debug("Blocking UDP traffic for unauthenticated user " + address);
        session.expireClient();
        session.expireServer();
        session.release();
    }

///// PRIVATE stuff ----------------------------------------------

}
