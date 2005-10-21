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
import com.metavize.mvvm.tran.Transform;

public class IDSDetectionEngine {

	private int 	        maxChunks 	= 8;
	private IDSSettings 	settings 	= null;
	private IDSRuleManager 	rules 		= new IDSRuleManager();
	
	private IDSTransformImpl 	transform;
	
	//Er - I need to remove stuff from the seesion Map??
	Map<Integer,IDSSessionInfo> 		sessionInfoMap 	= new ConcurrentHashMap<Integer,IDSSessionInfo>();
	Map<Integer,List<IDSRuleHeader>> 	portS2CMap      = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
	Map<Integer,List<IDSRuleHeader>> 	portC2SMap 	= new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
	
	private static final Logger log = Logger.getLogger(IDSDetectionEngine.class);
	static {
		log.setLevel(Level.INFO);
	}
	
	/*private static IDSDetectionEngine instance = new IDSDetectionEngine();
	public  static IDSDetectionEngine instance() {
		if(instance == null) 
			instance = new IDSDetectionEngine();
		return instance;
	}*/

	public IDSDetectionEngine() {
		//The Goggles! They do nothing!
		/*String test = "alert tcp 10.0.0.40-10.0.0.101 any -> 66.35.250.0/24 80 (content:\"slashdot\"; msg:\"OMG teH SLASHd0t\";)";
		String tesT = "alert tcp 10.0.0.1/24 any -> any any (content: \"spOOns|FF FF FF FF|spoons\"; msg:\"Matched binary FF FF FF and spoons\"; nocase;)";
		String TesT = "alert tcp 10.0.0.1/24 any -> any any (uricontent:\"slashdot\"; nocase; msg:\"Uricontent matched\";)";
		try {
			rules.addRule(test);
			rules.addRule(tesT);
			rules.addRule(TesT);
		} catch (ParseException e) {
			log.warn("Could not parse rule; " + e.getMessage());
		}*/
												
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

	public void updateUICount(int counter) {
		transform.incrementCount(counter);
	}

	public void setTransform(IDSTransformImpl transform) {
		this.transform = transform;
	}
	public void onReconfigure() {
		portC2SMap = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
		portS2CMap = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
		
		rules.onReconfigure();
		log.debug("Done with reconfigure");
	}

	public void updateRule(IDSRule rule) {
		try {
			rules.updateRule(rule);
		} catch (ParseException e) {
			log.warn("Could not parse rule; " + e.getMessage());
		} catch (Exception e) {
			log.error("Some sort of really bad exception: " + e.getMessage());
			log.error("For rule: " + rule);
		}	
	}
	
	//Deprecating?
	public boolean addRule(IDSRule rule) {
		try {
			return (rules.addRule(rule));
		} catch (ParseException e) { 
			log.warn("Could not parse rule; " + e.getMessage()); 
		} catch (Exception e) {
			log.error("Some sort of really bad exception: " + e.getMessage());
			log.error("For rule: " + rule);
		}
		return false;
	}

	public void processNewSession(IPNewSessionRequest session, Protocol protocol) {
		
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
				protocol, session.serverAddr(), session.serverPort(),
				session.clientAddr(), session.clientPort(), s2cList);
			
		if(c2sSignatures.size() > 0 || s2cSignatures.size() > 0) {
			IDSSessionInfo info = getSessionInfo(session.id());
			
			//I need to fix uricontent
			if(info == null) {
				info = new IDSSessionInfo();
				info.setC2SSignatures(c2sSignatures);
				info.setS2CSignatures(s2cSignatures);
			}
			else {
				info.setC2SSignatures(c2sSignatures);
				info.setS2CSignatures(s2cSignatures);
			}
			session.attach(info);
		}
		else
			session.release();
		//return false; // Fix me - not sure what I want to return
	}

	public IDSRuleManager getRulesForTesting() {
		return rules;
	}
	
	//In process of fixing this
	public void handleChunk(IPDataEvent event, IPSession session, boolean isServer) {
		try {
			long startTime = System.nanoTime();
		
			SessionStats stats = session.stats();
			if(stats.s2tChunks() > maxChunks || stats.c2tChunks() > maxChunks)
				session.release();
		
			IDSSessionInfo info = (IDSSessionInfo) session.attachment();
		
			info.setSession(session);
			info.setEvent(event);
			info.setFlow(isServer);
		
			if(isServer)
				info.processC2SSignatures();
			else
				info.processS2CSignatures();
		
			log.debug("Time: " + (float)(System.nanoTime() - startTime)/1000000f);
		}
		catch (Exception e) {
			log.error("Erron parseing chunk: " +e.getMessage());
		}
	}

	public void mapSessionInfo(int id, IDSSessionInfo info) {
		sessionInfoMap.put(id,info);
	}

	public IDSSessionInfo getSessionInfo(int id) {
		return sessionInfoMap.get(id);
	}
}
