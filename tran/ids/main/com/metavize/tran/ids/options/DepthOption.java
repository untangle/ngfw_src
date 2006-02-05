package com.metavize.tran.ids.options;

import java.util.regex.*;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;

import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tapi.event.*;

public class DepthOption extends IDSOption {

    private static final Logger logger = Logger.getLogger(DepthOption.class);
				
    public DepthOption(IDSRuleSignature signature, String params) throws ParseException {
        super(signature, params);
        ContentOption option = (ContentOption) signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set depth for sig: " + signature.rule().getText());
            signature.remove(true);
            return;	
        }
		
        int depth = 0;
        try {
            depth = Integer.parseInt(params);
        } catch (Exception e) { 
            throw new ParseException("Not a valid Offset argument: " + params);
        }
        option.setDepth(depth);	
    }
}
