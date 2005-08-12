package com.metavize.tran.ids;

import java.nio.*;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.PortRange;

public class IDSDetectionEngine {

	private static final Logger log = Logger.getLogger(IDSDetectionEngine.class);
	static {
		log.setLevel(Level.ERROR);
	}
		
	private IDSRules rules = new IDSRules();
	private List<IDSRuleHeader> ruleList;
	
	private static IDSDetectionEngine instance = null; 
	public static IDSDetectionEngine instance() {
		if(instance == null) 
			instance = new IDSDetectionEngine();

		return instance;
	}

	private IDSDetectionEngine() {
		String test = "alert tcp 10.0.0.40-10.0.0.101 any -> 66.35.250.0/24 80 ( test: hi; content: slashdot; msg:\"OMG teH SLASHd0t\";)";
		addRule(test);
	}

	public void addRule(String rule) {
		try {
			rules.addRule(rule);
		} catch (ParseException e) { log.warn("Could not parse rule; " + e.getMessage()); }		
	}

	public boolean processNewSession(IPNewSessionRequest session, Protocol protocol) {
		List<IDSRuleSignature> signatures = rules.matchesHeader(protocol, session.clientAddr(), session.clientPort(), session.serverAddr(), session.serverPort());
		if(signatures.size() > 0) {
			IDSSessionInfo info = new IDSSessionInfo(signatures);
			session.attach(info);
		}
		else
			session.release();
		return false; // Fix me - not sure what I want to return
	}
	
	public void handleChunk(IPDataEvent event, IPSession session) {
		IDSSessionInfo info = (IDSSessionInfo) session.attachment();
		
		info.setSession(session);
		info.setEvent(event);

		info.processSignatures();
	}
}
