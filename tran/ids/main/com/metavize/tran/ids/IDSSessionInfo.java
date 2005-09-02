package com.metavize.tran.ids;

import java.util.List;
import java.util.Iterator;
import com.metavize.mvvm.tapi.IPSession;
import com.metavize.mvvm.tapi.event.*;

public class IDSSessionInfo {
	
	private 	List<IDSRuleSignature> 	c2sSignatures;
	private 	List<IDSRuleSignature>	s2cSignatures;
	private 	IPSession 				session;
	private 	IPDataEvent 			event;
	private 	String 					uriPath;
	private 	boolean 				isServer;

	public IDSSessionInfo() { };
	
	public void setUriPath(String path) {
		uriPath = path;
	}

	public String  getUriPath() {
		return uriPath;
	}
	/**Do i need to set session data? I dont think so..
	 * Check later.
	 */
	public void setSession(IPSession session) {
		this.session = session;
	}

	public IPSession getSession() {
		return session;
	}
	
	public void setC2SSignatures(List<IDSRuleSignature> signatures) {
		this.c2sSignatures = signatures;
	}

	public void setS2CSignatures(List<IDSRuleSignature> signatures) {	
		this.s2cSignatures = signatures;
	}
	public void setEvent(IPDataEvent event) {
		this.event = event;
	}
	
	public IPDataEvent getEvent() {
		return event;
	}

	public void setFlow(boolean isServer) {
		this.isServer = isServer;
	}

	public boolean isServer() {
		return isServer;
	}

	public void processC2SSignatures() {
		for(IDSRuleSignature sig : c2sSignatures)
			sig.execute(this);
	}
	
	public void processS2CSignatures() {
		for(IDSRuleSignature sig : s2cSignatures)
			sig.execute(this);
	}
	
	/**Debug methods*/
	public boolean testSignature(int num) {
		return c2sSignatures.get(num).execute(this);
	}

	public IDSRuleSignature getSignature(int num) {
		return c2sSignatures.get(num);
	}
	// */
}
