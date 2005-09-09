package com.metavize.tran.ids.options;

import java.util.regex.*;
import java.nio.ByteBuffer;

import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.mvvm.tapi.event.*;

public class MsgOption extends IDSOption {
	
	public MsgOption(IDSRuleSignature signature, String params) {
		super(signature, params);
		signature.setMessage(params);	
	}

	public boolean runnable() {
		return false;
	}

	public boolean run() {
		return true;
	}
}
