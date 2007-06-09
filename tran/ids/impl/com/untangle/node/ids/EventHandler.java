/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.ids;

import com.untangle.uvm.tapi.*;
import com.untangle.uvm.tapi.event.*;

public class EventHandler extends AbstractEventHandler {

    private IDSDetectionEngine idsEngine;

    public EventHandler(IDSNodeImpl node) {
        super(node);
        idsEngine = node.getEngine();
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event) throws MPipeException {
        handleNewSessionRequest(event.sessionRequest(), Protocol.TCP);
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event) throws MPipeException {
        handleNewSessionRequest(event.sessionRequest(), Protocol.UDP);
    }

    private void handleNewSessionRequest(IPNewSessionRequest request, Protocol protocol) {
        idsEngine.processNewSessionRequest(request, protocol);
    }

    public void handleTCPNewSession(TCPSessionEvent event) throws MPipeException {
        handleNewSession(event.session(), Protocol.TCP);
    }

    public void handleUDPNewSession(UDPSessionEvent event) throws MPipeException {
        handleNewSession(event.session(), Protocol.UDP);
    }

    private void handleNewSession(IPSession session, Protocol protocol) {
        idsEngine.processNewSession(session, protocol);
    }

    public void handleTCPFinalized(TCPSessionEvent event) throws MPipeException {
        handleFinalized(event.session(), Protocol.TCP);
    }

    public void handleUDPFinalized(UDPSessionEvent event) throws MPipeException {
        handleFinalized(event.session(), Protocol.UDP);
    }

    private void handleFinalized(IPSession session, Protocol protocol) {
        idsEngine.processFinalized(session, protocol);
    }

    /*  public void handleTCPNewSession(TCPSessionEvent event) {

    }

    public void handleUDPNewSession(UDPSessionEvent event) {
    //UDPSession sess = event.session();
    }*/

    public IPDataResult handleTCPClientChunk(TCPChunkEvent event) {
        idsEngine.handleChunk(event, event.session(), false);
        return IPDataResult.PASS_THROUGH;
    }

    public IPDataResult handleTCPServerChunk(TCPChunkEvent event) {
        idsEngine.handleChunk(event, event.session(), true);
        return IPDataResult.PASS_THROUGH;
    }

    public void handleUDPClientPacket(UDPPacketEvent event) throws MPipeException {
        idsEngine.handleChunk(event, event.session(),false);
        super.handleUDPClientPacket(event);
    }

    public void handleUDPServerPacket(UDPPacketEvent event) throws MPipeException {
        idsEngine.handleChunk(event, event.session(),true);
        super.handleUDPServerPacket(event);
    }
    /*
      public void handleTCPFinalized(TCPChunkEvent event) {}
    */
}
