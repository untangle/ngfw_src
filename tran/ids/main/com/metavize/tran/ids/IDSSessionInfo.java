package com.metavize.tran.ids;

import java.util.List;
import java.util.Iterator;
import com.metavize.mvvm.tapi.IPSession;
import com.metavize.mvvm.tapi.event.*;

public class IDSSessionInfo {
	
	private List<IDSRuleSignature> signatures;
	private IPSession session;
	private IPDataEvent event;

	public IDSSessionInfo(List<IDSRuleSignature> signatures) {
		this.signatures = signatures;
	}

	public void setSession(IPSession session) {
		this.session = session;
	}

	public void setEvent(IPDataEvent event) {
		this.event = event;
	}

	public IPDataEvent getEvent() {
		return event;
	}

	public void processSignatures() {
		Iterator it = signatures.iterator();
		while(it.hasNext()) {
			IDSRuleSignature signature = (IDSRuleSignature)it.next();
			signature.execute(this);
		}
	}		
}
