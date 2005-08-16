package com.metavize.tran.ids;

import java.util.List;
import java.util.Iterator;
import com.metavize.mvvm.tapi.IPSession;
import com.metavize.mvvm.tapi.event.*;

public class IDSSessionInfo {
	
	private List<IDSRuleSignature> signatures;
	private IPSession session;
	private IPDataEvent event;
	private String uriPath;
	private boolean isServer;

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
	
	public void setSignatures(List<IDSRuleSignature> signatures) {
		this.signatures = signatures;
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

	public void processSignatures() {
		Iterator it = signatures.iterator();
		while(it.hasNext()) {
			IDSRuleSignature signature = (IDSRuleSignature)it.next();
			signature.execute(this);
		}
	}
	
	/**Debug methods*/
	public boolean testSignature(int num) {
		return signatures.get(num).execute(this);
	}

	public IDSRuleSignature getSignature(int num) {
		return signatures.get(num);
	}
	// */
}
