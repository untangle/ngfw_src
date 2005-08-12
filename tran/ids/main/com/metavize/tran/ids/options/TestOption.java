package com.metavize.tran.ids.options;

import com.metavize.tran.ids.IDSRuleSignature;

public class TestOption extends IDSOption {

	public TestOption(IDSRuleSignature signature, String params) {
		super(signature, params);
	}

	public boolean run() {
		System.out.println("Test Option works :D");
		return true;
	}
}
