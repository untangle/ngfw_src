package com.metavize.tran.ids;

import java.nio.*;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.PortRange;

public class IDSDetectionEngine {

	private int maxChunks = 8;
	private IDSSettings settings = null;
	private IDSRuleManager rules = new IDSRuleManager();
	Map<Integer,IDSSessionInfo> sessionInfoMap = new ConcurrentHashMap<Integer,IDSSessionInfo>();
	Map<Integer,List<IDSRuleHeader>> portS2CMap = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
	Map<Integer,List<IDSRuleHeader>> portC2SMap = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
	
	private static final Logger log = Logger.getLogger(IDSDetectionEngine.class);
	static {
		log.setLevel(Level.DEBUG);
	}	
	private static IDSDetectionEngine instance = new IDSDetectionEngine();//null; 
	public static IDSDetectionEngine instance() {
		if(instance == null) 
			instance = new IDSDetectionEngine();

		return instance;
	}

	private IDSDetectionEngine() {
		String test = "alert tcp 10.0.0.40-10.0.0.101 any -> 66.35.250.0/24 80 (content:\"slashdot\"; msg:\"OMG teH SLASHd0t\";)";
		String tesT = "alert tcp 10.0.0.1/24 any -> any any (content: \"spOOns|FF FF FF FF|spoons\"; msg:\"Matched binary FF FF FF and spoons\"; nocase;)";
		String TesT = "alert tcp 10.0.0.1/24 any -> any any (uricontent:\"slashdot\"; nocase; msg:\"Uricontent matched\";)";
		addRule(test);
		addRule(tesT);
		addRule(TesT);
	}

	public IDSSettings getSettings() {
		return settings;
	}

	public void setSettings(IDSSettings settings) {
		this.settings = settings;
	}
	
	//fix this - settigns?
	public void setMaxChunks(int max) {
		maxChunks = max;
	}

	public int getMaxChunks() {
		return maxChunks;
	}
	
	public boolean addRule(String rule) {
		try {
			return (rules.addRule(rule));
		} catch (ParseException e) { 
			log.warn("Could not parse rule; " + e.getMessage()); 
		} catch (Exception e) {
			log.error("Some sort of really bad exception: " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	public boolean processNewSession(IPNewSessionRequest session, Protocol protocol) {
		long startTime = System.nanoTime();
		
		//Get Mapped list
		List<IDSRuleHeader> c2sList = portC2SMap.get(session.serverPort());
		List<IDSRuleHeader> s2cList = portS2CMap.get(session.serverPort());
		
		if(c2sList == null) {
			c2sList = rules.matchingPortsList(session.serverPort(), IDSRuleManager.TO_SERVER);
			portC2SMap.put(session.serverPort(),c2sList);
			
			log.debug("\nc2sList Size: "+c2sList.size() + " For port: "+session.serverPort());
		}
		
		if(s2cList == null) {
			s2cList = rules.matchingPortsList(session.serverPort(), IDSRuleManager.TO_CLIENT);
			portS2CMap.put(session.serverPort(),s2cList);
			
			log.debug("\ns2cList Size: "+s2cList.size() + " For port: "+session.serverPort());
		}
		
		//Check matches
		List<IDSRuleSignature> c2sSignatures = rules.matchesHeader(
				protocol, session.clientAddr(), session.clientPort(), 
				session.serverAddr(), session.serverPort(), c2sList);

		List<IDSRuleSignature> s2cSignatures = rules.matchesHeader(
				protocol, session.clientAddr(), session.clientPort(),
				session.serverAddr(), session.serverPort(), s2cList);
		
		if(c2sSignatures.size() > 0) {
			IDSSessionInfo info = getSessionInfo(session.id());
			
			//I need to fix uricontent
			if(info == null) {
				info = new IDSSessionInfo();
				info.setc2sSignatures(c2sSignatures);
				info.sets2cSignatures(s2cSignatures);
			}
			else {
				info.setc2sSignatures(c2sSignatures);
				info.sets2cSignatures(s2cSignatures);
			}
			session.attach(info);
		}
		else
			session.release();
		
		log.debug("Time NewSession: " + (float)(System.nanoTime() - startTime)/1000000f);
		return false; // Fix me - not sure what I want to return
	}

	public IDSRuleManager getRulesForTesting() {
		return rules;
	}
	
	//In process of fixing this
	public void handleChunk(IPDataEvent event, IPSession session, boolean isServer) {
		long startTime = System.nanoTime();
		SessionStats stats = session.stats();
		if(stats.s2tChunks() > maxChunks || stats.c2tChunks() > maxChunks)
			session.release();
		
		IDSSessionInfo info = (IDSSessionInfo) session.attachment();
		
		info.setSession(session);
		info.setEvent(event);
		info.setFlow(isServer);

		info.processC2SSignatures();
		log.debug("Time HandleChunk: " + (float)(System.nanoTime() - startTime)/1000000f);
	}

	public void mapSessionInfo(int id, IDSSessionInfo info) {
		sessionInfoMap.put(id,info);
	}

	public IDSSessionInfo getSessionInfo(int id) {
		return sessionInfoMap.get(id);
	}
}
