package com.metavize.tran.ids;

import java.util.regex.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.*;
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
	public static final String[] ACTIONS = { "alert", "log" };
	
	private IDSRuleSignature newSignature = null;
	private List<IDSRuleHeader> knownHeaders = Collections.synchronizedList(new LinkedList<IDSRuleHeader>());
	
	private static Pattern variablePattern = Pattern.compile("\\$[^ \n\r\t]+");
	public static List<IDSVariable> defaultVariables = new ArrayList<IDSVariable>(); 
	static {
		//Overwrite any replacement to external or home net with the internal IPManger
		//To automatically track any changes made to the ip address
		//defaultVariables.add(new IDSVariable("$EXTERNAL_NET",""+0xDEAD,"This is a description"));
		//defaultVariables.add(new IDSVariable("$HOME_NET", ""+0xBEEF,"This is a description"));
		
		defaultVariables.add(new IDSVariable("$HTTP_PORTS", ":80","This is a description"));
		defaultVariables.add(new IDSVariable("$HTTP_SERVERS", "10.0.0.1/24","This is a description"));
		defaultVariables.add(new IDSVariable("$SMTP_SERVERS", "any","This is a description"));
		defaultVariables.add(new IDSVariable("$SSH_PORTS", "any","This is a description"));
		defaultVariables.add(new IDSVariable("$SQL_SERVERS", "any","This is a description"));
		defaultVariables.add(new IDSVariable("$TELNET_SERVERS", "any","This is a description"));
		defaultVariables.add(new IDSVariable("$ORACLE_PORTS", "any","This is a description"));
		defaultVariables.add(new IDSVariable("$AIM_SERVERS", "any","This is a description"));
	}
																													
	private static final Logger log = Logger.getLogger(IDSRuleManager.class);
	static {
		log.setLevel(Level.INFO);
	}
	public IDSRuleManager() {
	}

	public boolean addRule(String rule) throws ParseException {
		
		if(rule.length() <= 0 || rule.charAt(0) == '#')
			return false;
		rule = substituteVariables(rule);
		String ruleParts[] 			= IDSStringParser.parseRuleSplit(rule);
		IDSRuleHeader header		= IDSStringParser.parseHeader(ruleParts[0]);
		IDSRuleSignature signature	= IDSStringParser.parseSignature(ruleParts[1], header.getAction());
	
		signature.setToString(ruleParts[1]);
	
		if(!signature.remove()) {
			for(IDSRuleHeader known : knownHeaders) {
				if(known.equals(header)) {
					known.addSignature(signature);
					newSignature = signature;
					return true;
				}
			}
			header.addSignature(signature);
			newSignature = signature;
		////////////////////////////////////////	System.out.println(header);
			knownHeaders.add(header);
			return true;
		}
		return false;
	}

	public List<IDSRuleHeader> matchingPortsList(int port, boolean toServer) {
		List<IDSRuleHeader> returnList = new LinkedList();
		synchronized(knownHeaders) {
			for(IDSRuleHeader header : knownHeaders) {
				if(header.portMatches(port, toServer)) {
					returnList.add(header);
					//  System.out.println("\n\n"+header+"\n"+header.getSignature());
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
		//System.out.println("Total List size: "+matchList.size()); /** *****************************************/
	
		synchronized(matchList) {
			for(IDSRuleHeader header : matchList) {
				if(header.matches(protocol, clientAddr, clientPort, serverAddr, serverPort))
					returnList.addAll(header.getSignatures());
			}
		}
		//System.out.println("Signature List Size: "+returnList.size()); /** *****************************************/
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
		//Set 
		string = string.replaceAll("\\$EXTERNAL_NET",IDSStringParser.EXTERNAL_IP);
		string = string.replaceAll("\\$HOME_NET",IDSStringParser.HOME_IP);
		
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
