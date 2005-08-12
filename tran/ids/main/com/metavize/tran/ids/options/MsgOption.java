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
		int first = params.indexOf("\"");
		int last = params.lastIndexOf("\"");	
		if(first >= 0 && last > first) {
			message = params.substring(first+1,last);
		}
		else
			message = params;
		super.getSignature().setMessage(message);	
	}

	public boolean runnable() {
		return false;
	}

	public boolean run() {
		return true;
	}
}
