package com.metavize.tran.ids;

import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;

public class EventHandler extends AbstractEventHandler {

	private IDSDetectionEngine idsEngine;
	
	public EventHandler() {
		idsEngine = IDSDetectionEngine.instance();
	}

	public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event) throws MPipeException {
		handleNewSessionRequest(event.sessionRequest(), Protocol.TCP);
	}

	public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event) throws MPipeException {
		handleNewSessionRequest(event.sessionRequest(), Protocol.UDP);
	}

	private void handleNewSessionRequest(IPNewSessionRequest request, Protocol protocol) {
		idsEngine.processNewSession(request, protocol);
	}
	public void handleTCPNewSession(TCPSessionEvent event) {
		
	}

	public void handleUDPNewSession(UDPSessionEvent event) {
		UDPSession sess = event.session();
	}

	public IPDataResult handleTCPClientChunk(TCPChunkEvent event) {
			if(event.session().attachment() != null) // is there a better way to test this? Like ignore sessions wtih no matches?
				idsEngine.handleChunk(event, event.session(), false);
			return IPDataResult.PASS_THROUGH;
	}

	public IPDataResult handleTCPServerChunk(TCPChunkEvent event) {
			 if(event.session().attachment() != null) 
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

	public void handleTCPFinalized(TCPChunkEvent event) {
	}
}
