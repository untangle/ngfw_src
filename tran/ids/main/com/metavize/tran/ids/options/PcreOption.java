package com.metavize.tran.ids.options;

import java.util.regex.*;
import java.nio.ByteBuffer;
import com.metavize.tran.ids.IDSRuleSignature;

public class PcreOption extends IDSOption {

	private Pattern pcrePattern;

	public PcreOption(IDSRuleSignature signature, String params) {
		super(signature, params);
		
		int beginIndex = params.indexOf("/");
		int endIndex = params.lastIndexOf("/");
		
		if(beginIndex == endIndex)
			beginIndex = 0;

		if(beginIndex >= 0 && endIndex > beginIndex) {
			String pattern = params.substring(beginIndex+1,endIndex);
			String options = params.substring(endIndex);
			int flag = 0;
			options = options.toLowerCase();
			if(options.contains("i")) 
				flag = flag | Pattern.CASE_INSENSITIVE;
			if(options.contains("s"))
				flag = flag | Pattern.DOTALL;
			if(options.contains("m"))
				flag = flag | Pattern.MULTILINE;
			if(options.contains("x"))
				flag = flag | Pattern.COMMENTS;
			try {
				pcrePattern = Pattern.compile(pattern, flag);
			} catch(Exception e) {
				signature.remove(true);
			}
		}
	}

	public boolean runnable() {
		return true;
	}
	public boolean run() {
		ByteBuffer eventData = getSignature().getSessionInfo().getEvent().data();
		String data = new String(eventData.array());
		if(pcrePattern == null) {
			System.out.println("pcrePattern is null\n\n"+getSignature());
			return false;
		}
		return negationFlag() ^ pcrePattern.matcher(data).find();
	}
}
