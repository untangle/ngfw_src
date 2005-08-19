package com.metavize.tran.ids.options;

import com.metavize.tran.ids.IDSRuleSignature;

public class DistanceOption extends IDSOption {

	public DistanceOption(IDSRuleSignature signature, String params) {
		super(signature, params);
		int distance = Integer.parseInt(params);
		IDSOption option = signature.getOption("ContentOption",this);
		if(option != null) {
			ContentOption content = (ContentOption) option;
			content.setDistance(distance);
		}
				
	}

	public boolean runnable() {
		return false;
	}
	
	public boolean run() {
		return false;
	}
}
