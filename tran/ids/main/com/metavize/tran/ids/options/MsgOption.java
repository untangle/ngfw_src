package com.metavize.tran.ids.options;

import java.util.regex.*;
import java.nio.ByteBuffer;

import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.mvvm.tapi.event.*;

public class MsgOption extends IDSOption {

	private IDSRuleSignature signature;
	private String message;
	
	public MsgOption(IDSRuleSignature signature, String params) {
		super(signature, params);
		this.signature = signature;
		signature.setMessage(message);
		message = params;	
	}

	public boolean run() {
		return true;
	}
}
