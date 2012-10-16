/**
 * $Id: CaptureTrafficHandler.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture; // IMPL

import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;
import com.untangle.uvm.vnet.event.UDPPacketEvent;
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
    public void handleTCPNewSession(TCPSessionEvent event)
    {
        NodeTCPSession session = event.session();
        String clientAddr = session.getClientAddr().getHostAddress().toString();
        String serverAddr = session.getServerAddr().getHostAddress().toString();

        CaptureUserEntry user = node.captureUserTable.searchByAddress(clientAddr);

        // if we have an authenticated user release session and allow traffic
        if (user != null)
        {
            user.updateActivityTimer();
            node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
            session.release();
            return;
        }

        // check all the rules to see if traffic is allowed
        if (node.isSessionAllowed(clientAddr,serverAddr) == true)
        {
            // TODO event log here
            node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
            session.release();
            return;
        }

        // traffic not yet allowed so check for and pass web traffic so
        // the http casing can create the redirect to the captive page
        if (session.getServerPort() == 80)
        {
            node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
            session.release();
            return;
        }

        // not allowed and not web traffic so we block
        node.incrementBlinger(CaptureNode.BlingerType.SESSBLOCK,1);
        session.resetClient();
        session.resetServer();
        session.release();
    }

///// UDP stuff --------------------------------------------------

    @Override
    public void handleUDPNewSession(UDPSessionEvent event)
    {
        NodeUDPSession session = event.session();
        String clientAddr = session.getClientAddr().getHostAddress().toString();
        String serverAddr = session.getServerAddr().getHostAddress().toString();
        CaptureUserEntry user = node.captureUserTable.searchByAddress(clientAddr);

        // if we have an authenticated user release session and allow traffic
        if (user != null)
        {
            user.updateActivityTimer();
            node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
            session.release();
            return;
        }

        // check all the rules to see if traffic is allowed
        if (node.isSessionAllowed(clientAddr,serverAddr) == true)
        {
            // TODO event log here
            node.incrementBlinger(CaptureNode.BlingerType.SESSALLOW,1);
            session.release();
            return;
        }

        // traffic not yet allowed so we hook DNS traffic which will
        // allow us to do the lookup ourselves.  this will ensure we
        // can't be circumvented by creative UDP port 53 traffic and it
        // will allow HTTP requests to become established which is
        // required for the http-casing to do the redirect
        if (session.getServerPort() == 53)
        {
            DNSPacket packet = new DNSPacket();
            session.attach(packet);
            return;
        }

        // not allow and not DNS traffic so block
        node.incrementBlinger(CaptureNode.BlingerType.SESSBLOCK,1);
        session.expireClient();
        session.expireServer();
        session.release();
    }

    @Override
    public void handleUDPClientPacket(UDPPacketEvent event)
    {
        NodeUDPSession session = event.session();
        DNSPacket packet = (DNSPacket)session.attachment();
        InetAddress addr = null;
        session.attach(null);

        // extract the DNS query from the client packet
        packet.ExtractQuery(event.data().array(),event.data().limit());
        logger.debug(packet.toString());

            // this handler will only see UDP packets with a target port
            // of 53 sent from unauthenticated client so if it doesn't seem
            // like a valid DNS query we just ignore and block
            if (packet.isValidDNSQuery() != true)
            {
                node.incrementBlinger(CaptureNode.BlingerType.SESSBLOCK,1);
                session.expireClient();
                session.expireServer();
                session.release();
            }

        // we have a valid query so lets lookup the address
        try
        {
            addr = InetAddress.getByName(packet.getQname());
        }

        // resolution failed so addr will be null and the DNS packet
        // response generator will substitute 0.0.0.0
        catch (Exception e)
        {
            logger.info("Unable to resolve " + packet.getQname());
        }

        // craft a DNS response pointing to the address we got back
        ByteBuffer bb = packet.GenerateResponse(addr);

        // send the packet to the client
        session.sendClientPacket(bb,event.header());

        // increment our counter and release the session
        node.incrementBlinger(CaptureNode.BlingerType.SESSPROXY,1);
        session.release();
    }
}
