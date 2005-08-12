package com.metavize.tran.ids;

import java.util.Vector;
import java.util.List;
import java.util.Iterator;
import java.util.Random;

import java.net.InetAddress;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.ParseException;

public class IDSTest {

	private static final Logger log = Logger.getLogger(IDSTest.class);
	private IDSRules rules = new IDSRules();
	
	static {
		log.setLevel(Level.ERROR);
	}
	public IDSTest() {
		log.debug("Testing");
	}

	public boolean runTest() {
		log.warn("\n\n******************************************************");
		boolean passGenTest = generateRuleTest();
		runRulesMatchTest();
		generateRandomRuleHeaders(5000);
		runTimeTest(5);
		return true;
	}

	public boolean generateRuleTest() {
		String testValidStrings[] 	= { 
			"alert tcp any any -> any any (msg:Rule One;)",
			"alert tcp 10.0.0.40-10.0.0.101 any -> 66.35.250.0/24 80 (msg:Rule twO;)",
			"alert tcp 10.0.0.101 !5000: -> 10.0.0.1/16 !80 (msg: Rule 3;)",
			"alert TCP 10.0.0.101 4000:5000 <> 10.0.0.1/24 :6000 (msg:  Rule 4;)",
			"alert tcp [10.0.0.101,192.168.1.1,10.0.0.44] !:80 -> any 80 (msg: Rule x5x     ; )"}; 
		
		for(int i=0; i < testValidStrings.length; i++) {
			try { 
				rules.addRule(testValidStrings[i]);
			} catch (ParseException e)  { log.error(e.getMessage()); }
		}
		return true;
	}

	private void runRulesMatchTest() {
		
		List<IDSRuleHeader> ruleList = rules.getHeaders();
		
		matchTest(ruleList.get(1), Protocol.TCP, "10.0.0.101", 33242, "66.35.250.8", 80, true);
		matchTest(ruleList.get(0), Protocol.TCP, "192.168.1.1", 33065, "66.33.22.111", 80, true);
		matchTest(ruleList.get(0), Protocol.UDP, "192.168.1.1", 33065, "66.33.22.111", 80, false);
		matchTest(ruleList.get(1), Protocol.TCP, "192.168.1.1", 33065, "66.33.22.111", 80, false);
		matchTest(ruleList.get(2), Protocol.TCP, "192.168.1.1", 33065, "66.33.22.111", 80, false);
		matchTest(ruleList.get(4), Protocol.TCP, "192.168.1.1", 33065, "66.33.22.111", 80, true);
		matchTest(ruleList.get(1), Protocol.TCP, "10.0.0.43", 1024, "10.0.0.101", 4747, false);
		matchTest(ruleList.get(4), Protocol.TCP, "10.0.0.43", 1024, "10.0.0.101", 4747, false);
		matchTest(ruleList.get(1), Protocol.TCP, "10.0.0.101",3232,"10.0.0.31",4999, false);
		matchTest(ruleList.get(2), Protocol.TCP, "10.0.0.101",3232,"10.0.0.31",4999, true);
		matchTest(ruleList.get(3), Protocol.TCP, "10.0.0.101",3232,"10.0.0.31",4999, false); 
	}

	private void matchTest(IDSRuleHeader header, Protocol protocol, String clientAddr, int clientPort, String serverAddr, int serverPort, boolean answer) {
		InetAddress clientAddress = null;
		InetAddress serverAddress = null;
		try {
			clientAddress = InetAddress.getByName(clientAddr);
			serverAddress = InetAddress.getByName(serverAddr);
		} catch( Exception e ) { log.error(e); }

		if(!checkAnswer(header.matches(protocol, clientAddress, clientPort, serverAddress, serverPort),answer))
			log.warn("Match Test Failed:\n"  + 
					"Client:" +clientAddress+":"+clientPort + 
					"\nServer:" +serverAddress+":"+serverPort +
					"\ndoes not match rule:\n" + header +"\n");
	}
	
	private boolean checkAnswer(boolean eval, boolean correct) {
		if(eval != correct) 
			log.warn("Evaluated: "+ eval+ " Should be: " + correct);
		return eval == correct;
	}

	private void runTimeTest(int seconds) {
		long stopTime = System.currentTimeMillis() + seconds*1000;
		int counter = 0;

		Random rand = new Random();
		while(stopTime > System.currentTimeMillis()) {
			try {
				InetAddress addr1 = InetAddress.getByName(rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256));
				InetAddress addr2 = InetAddress.getByName(rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256));
				rules.matchesHeader(Protocol.TCP, addr1, rand.nextInt(65536), addr2, rand.nextInt(65536));
			} catch (Exception e) {}
			counter++;
		}
		double timePerMatch = (double)seconds*1000/(double)counter;
		log.info("Completed " + counter+ " matches in " + seconds + " seconds "+" ("+timePerMatch+" ms/match"+").");
	}

	private void generateRandomRuleHeaders(int num) {
		long startTime = System.currentTimeMillis();
		Random rand = new Random();
		rules.clear();
		for(int i=0;i<num;i++) {

			String dir;
			String prot;
			String clientIP, serverIP;
			String clientPort, serverPort;
			if(rand.nextInt(2) == 0)
				dir = " ->";
			else
				dir = " <>";
			
			if(rand.nextInt(2) == 0)
				prot = " udp";
			else
				prot = " TCP";
			clientIP = getRandomIPAddress();
			serverIP = getRandomIPAddress();

			clientPort = getRandomPort();
			serverPort = getRandomPort();

			try {
				rules.addRule("alert"+prot+clientIP+clientPort+dir+serverIP+serverPort+" ( content: I like spoons; msg: This is just a test; invalidOption: this does nothing; invalidOption: this does nothing; nocase; )");
			} catch(ParseException e) { log.error("Could not parse rule; " + e.getMessage()); }
		}
		long endTime = System.currentTimeMillis() - startTime;
		log.info("Time it took to parse " + num +" rules: " + endTime + " milliseconds");
	}

	private String getRandomPort() {
		String str;
		Random rand = new Random();
		switch(rand.nextInt(3)) {
			case 0:
				str = "any";
				break;
			case 1:
				str = rand.nextInt(65536)+"";
				break;
			case 2:
				int port1 = rand.nextInt(65536);
				int port2 = rand.nextInt(65536);
				str = port1+":"+port2;
				break;
			default:
				str = "any";
		}
		return " "+str;
	}
	
	private String getRandomIPAddress() {
		String str;
		Random rand = new Random();
		switch(rand.nextInt(4))  {
			case 0:
				str = "any";
				break;
			case 1:
				str ="10.0.0.1/24";
				break;
			case 2:
				str = "[192.168.0.1,192.168.0.30,192.168.0.101]";						
				break;
			case 3:
				str = rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256)+"."+rand.nextInt(256);
				break;
			default:
				str = "any";
		}
		return " "+str;
	}		
}
