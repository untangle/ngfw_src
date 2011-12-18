/**
 * $Id$
 */
package com.untangle.node.ips;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.IPSession;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.event.IPDataResult;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPPacketEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;

public class EventHandler extends AbstractEventHandler
{
    private IpsDetectionEngine ipsEngine;

    public EventHandler(IpsNodeImpl node) {
        super(node);
        ipsEngine = node.getEngine();
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
        ipsEngine.processNewSessionRequest(request, protocol);
    }

    public void handleTCPNewSession(TCPSessionEvent event)
    {
        handleNewSession(event.session(), Protocol.TCP);
    }

    public void handleUDPNewSession(UDPSessionEvent event)
    {
        handleNewSession(event.session(), Protocol.UDP);
    }

    private void handleNewSession(IPSession session, Protocol protocol)
    {
        ipsEngine.processNewSession(session, protocol);
    }

    public void handleTCPFinalized(TCPSessionEvent event)
    {
        handleFinalized(event.session(), Protocol.TCP);
    }

    public void handleUDPFinalized(UDPSessionEvent event)
    {
        handleFinalized(event.session(), Protocol.UDP);
    }

    private void handleFinalized(IPSession session, Protocol protocol)
    {
        ipsEngine.processFinalized(session, protocol);
    }

    public IPDataResult handleTCPClientChunk(TCPChunkEvent event)
    {
        ipsEngine.handleChunk(event, event.session(), false);
        return IPDataResult.PASS_THROUGH;
    }

    public IPDataResult handleTCPServerChunk(TCPChunkEvent event)
    {
        ipsEngine.handleChunk(event, event.session(), true);
        return IPDataResult.PASS_THROUGH;
    }

    public void handleUDPClientPacket(UDPPacketEvent event)
    {
        ipsEngine.handleChunk(event, event.session(),false);
        super.handleUDPClientPacket(event);
    }

    public void handleUDPServerPacket(UDPPacketEvent event)
    {
        ipsEngine.handleChunk(event, event.session(),true);
        super.handleUDPServerPacket(event);
    }

}
