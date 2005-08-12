package com.metavize.tran.ids;

import java.util.List;
import java.util.Iterator;
import com.metavize.mvvm.tapi.IPSession;
import com.metavize.mvvm.tapi.event.*;

public class IDSSessionInfo {
	
	private List<IDSRuleSignature> signatures;
	private IPSession session;
	private IPDataEvent event;
	private boolean isServer;

	public IDSSessionInfo(List<IDSRuleSignature> signatures) {
		this.signatures = signatures;
	}
	/**Do i need to set session data? I dont think so..
	 * Check later.
	 */
	public void setSession(IPSession session) {
		this.session = session;
	}

	public void setEvent(IPDataEvent event) {
		this.event = event;
	}

	public void setFlow(boolean isServer) {
		this.isServer = isServer;
	}

	public IPDataEvent getEvent() {
		return event;
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
	
	public boolean testSignature(int num) {
		return signatures.get(num).execute(this);
	}
}
