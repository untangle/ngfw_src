package com.metavize.tran.ids.options;

import java.util.regex.*;
import java.nio.ByteBuffer;

import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tapi.event.*;

public class DepthOption extends IDSOption {

    public DepthOption(IDSRuleSignature signature, String params) throws ParseException {
        super(signature, params);
        ContentOption option = (ContentOption) signature.getOption("ContentOption",this);
        if(option == null) 
            return;	
		
        int depth = 0;
        try {
            depth = Integer.parseInt(params);
        } catch (Exception e) { 
            throw new ParseException("Not a valid Offset argument: " + params);
        }
        option.setDepth(depth);	
    }
}
