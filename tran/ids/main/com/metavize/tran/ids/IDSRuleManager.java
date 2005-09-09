package com.metavize.tran.ids;

import java.util.regex.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.net.InetAddress;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.PortRange;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.ParseException;

public class IDSRuleManager {

	public static final boolean TO_SERVER = true;	
	public static final boolean TO_CLIENT = false;

	public static final int ALERT = 0;
	public static final int LOG = 1;
	public static final int PASS = 2;
	public static final int BLOCK = 3;
	public static final String[] ACTIONS = { "alert","log","pass","block" };
	
	private IDSRuleSignature newSignature = null;
	private List<IDSRuleHeader> knownHeaders = Collections.synchronizedList(new LinkedList<IDSRuleHeader>());
	private Map<Long,IDSRule> knownRules = new ConcurrentHashMap<Long,IDSRule>();
	
	private static Pattern variablePattern = Pattern.compile("\\$[^ \n\r\t]+");
	public static List<IDSVariable> immutableVariables = new ArrayList<IDSVariable>(); 
	static {
		immutableVariables.add(new IDSVariable("$EXTERNAL_NET","Set by Edgeguard","This is a description"));
		immutableVariables.add(new IDSVariable("$HOME_NET","Set by EdgeGuard","This is a description"));
	}
	public static List<IDSVariable> defaultVariables = new ArrayList<IDSVariable>(); 
	static {
		defaultVariables.add(new IDSVariable("$HTTP_PORTS", "80","This is a description"));
		defaultVariables.add(new IDSVariable("$HTTP_SERVERS", "!any","This is a description"));
		defaultVariables.add(new IDSVariable("$SMTP_SERVERS", "!any","This is a description"));
		defaultVariables.add(new IDSVariable("$SSH_PORTS", "22","This is a description"));
		defaultVariables.add(new IDSVariable("$SQL_SERVERS", "!any","This is a description"));
		defaultVariables.add(new IDSVariable("$TELNET_SERVERS", "!any","This is a description"));
		defaultVariables.add(new IDSVariable("$ORACLE_PORTS", "!any","This is a description"));
		defaultVariables.add(new IDSVariable("$AIM_SERVERS", "!any","This is a description"));
	}
																													
	private static final Logger log = Logger.getLogger(IDSRuleManager.class);
	static {
		log.setLevel(Level.ALL);
	}
	public IDSRuleManager() {
	}

	public void onReconfigure() {
		for(IDSRule rule : knownRules.values()) {
			rule.remove(true);
		}
	}

	public void updateRule(IDSRule rule) throws ParseException {
		long id = rule.getKeyValue();
		IDSRule inMap = knownRules.get(id);
		if(inMap != null) {
			rule.remove(false);
			if(rule.getModified()) {	
				log.info("in getModified == true");
				rule.setModified(false);
				
				//Delete previous rule
				IDSRuleHeader header = inMap.getHeader();
				header.removeSignature(inMap.getSignature());
				
				if(header.signatureListIsEmpty()) {
					knownRules.remove(id);
					knownHeaders.remove(header);
				}
				
				//Add the modified rule
				log.info("Adding modified rule");
				addRule(rule);
			}
		}

		else {
		//	log.info("Does not contain - adding");
			rule.setModified(false);
			addRule(rule);
		}
		//remove all rules with remove == true
		//rule.setModified(false);
			
	}
	
	public boolean addRule(String rule, Long key) throws ParseException {
		IDSRule test = new IDSRule(rule,"Not set", "Not set");
		test.setKeyValue(key);
		return addRule(test);
	}
	public boolean addRule(IDSRule rule) throws ParseException {
		String ruleText = rule.getText();
		
		if(ruleText.length() <= 0 || ruleText.charAt(0) == '#')
			return false;
		
		ruleText = substituteVariables(ruleText);
		String ruleParts[] 			= IDSStringParser.parseRuleSplit(ruleText);
		IDSRuleHeader header		= IDSStringParser.parseHeader(ruleParts[0]);
		IDSRuleSignature signature	= IDSStringParser.parseSignature(ruleParts[1], header.getAction());
	
		signature.setToString(ruleParts[1]);
	
		if(!signature.remove()) {
			for(IDSRuleHeader known : knownHeaders) {
				if(known.equals(header)) {
					known.addSignature(signature);
					newSignature = signature;
					
					rule.setHeader(known);
					rule.setSignature(signature);
					knownRules.put(rule.getKeyValue(),rule);
					return true;
				}
			}
			header.addSignature(signature);
			newSignature = signature;
			knownHeaders.add(header);

			rule.setHeader(header);
			rule.setSignature(signature);
			knownRules.put(rule.getKeyValue(),rule);
			
			return true;
		}
		return false;
	}

	public boolean canParse(String text) {
		if(text.length() <= 0 || text.charAt(0) == '#')
			return false;

		text = substituteVariables(text);
		try {
			String ruleParts[]          = IDSStringParser.parseRuleSplit(text);
			IDSRuleHeader header        = IDSStringParser.parseHeader(ruleParts[0]);
			IDSRuleSignature signature  = IDSStringParser.parseSignature(ruleParts[1], header.getAction());
			
			if(!signature.remove()) {
				newSignature = signature;
				return true;
			}
			return false;			
		}
		catch(ParseException e) { 
			return false;
		}			
	}

	public List<IDSRuleHeader> matchingPortsList(int port, boolean toServer) {
		List<IDSRuleHeader> returnList = new LinkedList();
		synchronized(knownHeaders) {
			for(IDSRuleHeader header : knownHeaders) {
				if(header.portMatches(port, toServer)) {
					returnList.add(header);
				}
			}
		}
		return returnList;
	}
	
	public List<IDSRuleSignature> matchesHeader(
			Protocol protocol, InetAddress clientAddr, int clientPort, 
			InetAddress serverAddr, int serverPort) {
		
		return matchesHeader(protocol, clientAddr, clientPort, serverAddr, serverPort, knownHeaders);
	}
	
	public List<IDSRuleSignature> matchesHeader(
			Protocol protocol, InetAddress clientAddr, int clientPort, 
			InetAddress serverAddr, int serverPort, List<IDSRuleHeader> matchList) {
		
		List<IDSRuleSignature> returnList = new LinkedList();
		//log.debug("Total List size: "+matchList.size()); /** *****************************************/
	
		synchronized(matchList) {
			for(IDSRuleHeader header : matchList) {
				if(header.matches(protocol, clientAddr, clientPort, serverAddr, serverPort))
					returnList.addAll(header.getSignatures());
			}
		}
		//log.debug("Signature List Size: "+returnList.size()); /** *****************************************/
		return returnList;
	}

	/*For debug yo*/
	public List<IDSRuleHeader> getHeaders() {
		return knownHeaders;
	}

	public void clear() {
		knownHeaders.clear();
	}
	
	public IDSRuleSignature getNewestSignature() {
		return newSignature;
	}

	private String substituteVariables(String string) {
		string = string.replaceAll("\\$EXTERNAL_NET",IDSStringParser.EXTERNAL_IP);
		string = string.replaceAll("\\$HOME_NET",IDSStringParser.HOME_IP);
		
		//string = string.replaceAll("\\$HOME_NET","10.0.0.1/24");
		//string = string.replaceAll("\\$EXTERNAL_NET","!10.0.0.1/24");
		
		Matcher match = variablePattern.matcher(string);
		if(match.find()) {
			List<IDSVariable> varList;
			if(IDSDetectionEngine.instance().getSettings() == null)
				varList = defaultVariables;
			else {
				varList = (List<IDSVariable>) IDSDetectionEngine.instance().getSettings().getVariables();
			}
			for(IDSVariable var : varList) {
				string = string.replaceAll("\\"+var.getVariable(),var.getDefinition());
			}																		
		}
		return string;
	}
}
