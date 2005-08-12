package com.metavize.tran.ids.options;

import com.metavize.tran.ids.IDSRuleSignature;

public class TestOption extends IDSOption {

	public TestOption(IDSRuleSignature signature, String params) {
		super(signature, params);
	}

	public boolean runnable() {
		return false;
	}
	public boolean run() {
		return false;
	}
}
