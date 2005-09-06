package com.metavize.tran.ids;

import java.net.InetAddress;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.PortRange;
import com.metavize.mvvm.tran.firewall.IPMatcher;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;

public class IDSRuleHeader {
	/*
	private class SignatureLinkedList {
		
		private class Node {
			private Node next = null;
			public IDSRuleSignature signature;

			public Node(IDSRuleSignature signature) {
				this.signature = signature;
			}

			public void next(Node node) {
				next = node;
			}

			public Node next() {
				return next;
			}
		}

		private int size = 0;
		private Node rootNode = null;
		private Node currentNode = null;
		
		public SignatureLinkedList() { }
		
		public void add(IDSRuleSignature signature) {
			if(currentNode == null) {
				currentNode = new Node(signature);
				rootNode = currentNode;
			}
			else {
				Node nextNode = new Node(signature);
				currentNode.next(nextNode);
				currentNode = nextNode;
			}
		}

		public void add(SignatureLinkedList other) {
			currentNode.next(other.rootNode);
			currentNode = other.currnetNode;
		}
	}*/

	public static final boolean	IS_BIDIRECTIONAL = true;
	public static final boolean	IS_SERVER = true;
	
	private int 			action = 0;
	private Protocol		protocol;
	
	private List<IPMatcher>	clientIPList;
	private PortRange		clientPortRange;
	
	private boolean			bidirectional = false;
	
	private List<IPMatcher>	serverIPList;
	private PortRange 		serverPortRange;

	private	List<IDSRuleSignature> signatures = new LinkedList<IDSRuleSignature>();

	/**
	 * Negation Flags: flag XOR input = answer
	 * */
	private boolean 	clientIPFlag = false;
	private boolean   	clientPortFlag = false;
	private boolean  	serverIPFlag = false;
	private boolean		serverPortFlag = false;

	/**
	 * Directional opperator flag
	 */
	private boolean		swapFlag = false;
	
	public IDSRuleHeader(int action, boolean bidirectional, Protocol protocol,
				List<IPMatcher> clientIPList,	PortRange clientPortRange,
				List<IPMatcher> serverIPList,	PortRange serverPortRange) {
		
		this.bidirectional = bidirectional;
		this.protocol = protocol;
		this.clientIPList = clientIPList;
		this.serverIPList = serverIPList;
		
		this.clientPortRange = clientPortRange;
		this.serverPortRange = serverPortRange;
	}

	public boolean portMatches(int port, boolean toServer) {
		if(toServer)
			return serverPortFlag ^ serverPortRange.contains(port);
		else
			return clientPortFlag ^ clientPortRange.contains(port);
	}
			
	public boolean matches(Protocol protocol, InetAddress clientAddr, int clientPort, InetAddress serverAddr, int serverPort) {
		if(this.protocol != protocol)
			return false;
		
		/**Check Port Match*/
		boolean clientPortMatch = clientPortRange.contains(clientPort);
		boolean serverPortMatch = serverPortRange.contains(serverPort);
		boolean portMatch = (clientPortMatch ^ clientPortFlag) && (serverPortMatch ^ serverPortFlag);
		if(!portMatch && !bidirectional)
			return false;
		
		/**Check IP Match*/
		boolean clientIPMatch = false;
		Iterator<IPMatcher> clientIt = clientIPList.iterator();
		while(clientIt.hasNext() && !clientIPMatch)  {
			IPMatcher matcher = clientIt.next();
			clientIPMatch =  matcher.isMatch(clientAddr);
		}

		 boolean serverIPMatch = false;
		 Iterator<IPMatcher> serverIt = serverIPList.iterator();
		 while(serverIt.hasNext() && !serverIPMatch) {
			 IPMatcher matcher = serverIt.next();
			 serverIPMatch = matcher.isMatch(serverAddr);
		 }
		 boolean ipMatch = (clientIPMatch ^ clientIPFlag) && (serverIPMatch ^ serverIPFlag);
		
		 /**Check Directional flag*/
		 if(!(ipMatch && portMatch) && bidirectional && !swapFlag) {
			 swapFlag = true;
			 return matches(protocol, serverAddr, serverPort, clientAddr, clientPort);
		 }
		 
		 if(swapFlag)
			 swapFlag = false;

		return ipMatch && portMatch;
	}

	public void setNegationFlags(boolean clientIP, boolean clientPort, boolean serverIP, boolean serverPort) {
		
		clientIPFlag = clientIP;
		clientPortFlag = clientPort;
		serverIPFlag = serverIP;
		serverPortFlag = serverPort;
	}

	public void addSignature(IDSRuleSignature sig) {
		signatures.add(sig);
	}

	public boolean removeSignature(IDSRuleSignature sig) {
		return signatures.remove(sig);
	}

	public int getAction() {
		return action;
	}
	
	public List<IDSRuleSignature> getSignatures() {
		return signatures;
	}

	public boolean signatureListIsEmpty() {
		return signatures.isEmpty();
	}
	
	public boolean equals(IDSRuleHeader other) {
		boolean action = (this.action == other.action);
		boolean protocol = (this.protocol == other.protocol); // ?
		boolean clientPorts = (this.clientPortRange.equals(other.clientPortRange));
		boolean serverPorts = (this.serverPortRange.equals(other.serverPortRange));
		boolean serverIP = (this.serverIPList.equals(other.serverIPList));
		boolean clientIP = (this.serverIPList.equals(other.serverIPList));
		boolean direction = (this.bidirectional == other.bidirectional);

		return action && protocol && clientPorts && serverPorts && serverIP && clientIP && direction;
	}
	
	public String toString() {
		String str = "alert tcp ";
		if(clientIPFlag)
			str += "!";
		str += clientIPList + " ";
		if(clientPortFlag)
			str += "!";
		str += clientPortRange;
		if(bidirectional)
			str += " <> ";
		else
			str += " -> ";
		if(serverIPFlag)
			str += "!";
		str += serverIPList +" ";
		if(serverPortFlag)
			str += "!";
		str += serverPortRange;
		return str;
	}
}
