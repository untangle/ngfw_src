package com.metavize.tran.ids.options;

import com.metavize.tran.ids.IDSRuleSignature;

public class WithinOption extends IDSOption {

	public WithinOption(IDSRuleSignature signature, String params) {
		super(signature, params);
		int within = Integer.parseInt(params);
		IDSOption option = signature.getOption("ContentOption",this);
		if(option != null) {
			ContentOption content = (ContentOption) option;
			content.setWithin(within);
		}
				
	}

	public boolean runnable() {
		return false;
	}
	
	public boolean run() {
		return false;
	}
}
