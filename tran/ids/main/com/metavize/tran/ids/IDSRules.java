package com.metavize.tran.ids;

import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.net.InetAddress;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.PortRange;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.ParseException;

public class IDSRules {
	
	public static final int ALERT = 0;
	public static final int LOG = 1;
	public static final String[] ACTIONS = { "alert", "log" };
			
	private List<IDSRuleHeader> rules = new Vector<IDSRuleHeader>();
	
	private static final Logger log = Logger.getLogger(IDSRules.class);
	static {
		log.setLevel(Level.WARN);
	}
	public IDSRules() {
	}

	public void addRule(String rule) throws ParseException {
		
		/*Have to seperate Header from signature*/
		if(rule.charAt(0) == '#')
			return;
		rule = substituteVariables(rule);
		String ruleParts[] 			= IDSStringParser.parseRuleSplit(rule);
		IDSRuleHeader header		= IDSStringParser.parseHeader(ruleParts[0]);
		IDSRuleSignature signature	= IDSStringParser.parseSignature(ruleParts[1], header.getAction());
		
		//Might want to change this.
		header.setSignature(signature);
		rules.add(header);
	}

	public List<IDSRuleSignature> matchesHeader(Protocol protocol, InetAddress clientAddr, int clientPort, InetAddress serverAddr, int serverPort) {
		List<IDSRuleSignature> returnList = new LinkedList();
		
		Iterator<IDSRuleHeader> it = rules.iterator();
		while(it.hasNext()) {
			IDSRuleHeader header = it.next();
			if(header.matches(protocol, clientAddr, clientPort, serverAddr, serverPort))
				returnList.add(header.getSignature());
		}
		return returnList;
	}

	/*For debug yo*/
	public List<IDSRuleHeader> getHeaders() {
		return rules;
	}

	public void clear() {
		rules.clear();
	}

	private String substituteVariables(String string) {
		string = string.replaceAll("\\$EXTERNAL_NET","!10.0.0.1/24");
		string = string.replaceAll("\\$HOME_NET", "10.0.0.1/24");
		string = string.replaceAll("\\$HTTP_PORTS", "any");
		string = string.replaceAll("\\$SSH_PORTS", "any");
		string = string.replaceAll("\\$.*\b", "any");
		
		return string;
	}
}

