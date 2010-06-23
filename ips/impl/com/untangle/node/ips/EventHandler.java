/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.IPSession;
import com.untangle.uvm.vnet.MPipeException;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.event.IPDataResult;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPPacketEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;

public class EventHandler extends AbstractEventHandler {

    private IpsDetectionEngine ipsEngine;

    public EventHandler(IpsNodeImpl node) {
        super(node);
        ipsEngine = node.getEngine();
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event) throws MPipeException {
        handleNewSessionRequest(event.sessionRequest(), Protocol.TCP);
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event) throws MPipeException {
        handleNewSessionRequest(event.sessionRequest(), Protocol.UDP);
    }

    private void handleNewSessionRequest(IPNewSessionRequest request, Protocol protocol) {
        ipsEngine.processNewSessionRequest(request, protocol);
    }

    public void handleTCPNewSession(TCPSessionEvent event) throws MPipeException {
        handleNewSession(event.session(), Protocol.TCP);
    }

    public void handleUDPNewSession(UDPSessionEvent event) throws MPipeException {
        handleNewSession(event.session(), Protocol.UDP);
    }

    private void handleNewSession(IPSession session, Protocol protocol) {
        ipsEngine.processNewSession(session, protocol);
    }

    public void handleTCPFinalized(TCPSessionEvent event) throws MPipeException {
        handleFinalized(event.session(), Protocol.TCP);
    }

    public void handleUDPFinalized(UDPSessionEvent event) throws MPipeException {
        handleFinalized(event.session(), Protocol.UDP);
    }

    private void handleFinalized(IPSession session, Protocol protocol) {
        ipsEngine.processFinalized(session, protocol);
    }

    /*  public void handleTCPNewSession(TCPSessionEvent event) {

    }

    public void handleUDPNewSession(UDPSessionEvent event) {
    //UDPSession sess = event.session();
    }*/

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

    public void handleUDPClientPacket(UDPPacketEvent event) throws MPipeException 
    {
        ipsEngine.handleChunk(event, event.session(),false);
        super.handleUDPClientPacket(event);
    }

    public void handleUDPServerPacket(UDPPacketEvent event) throws MPipeException 
    {
        ipsEngine.handleChunk(event, event.session(),true);
        super.handleUDPServerPacket(event);
    }
    /*
      public void handleTCPFinalized(TCPChunkEvent event) {}
    */
}
