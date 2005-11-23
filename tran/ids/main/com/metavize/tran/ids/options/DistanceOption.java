package com.metavize.tran.ids.options;

import com.metavize.tran.ids.IDSRuleSignature;
import org.apache.log4j.Logger;

public class DistanceOption extends IDSOption {

    private static final Logger logger = Logger.getLogger(DistanceOption.class);
				
    public DistanceOption(IDSRuleSignature signature, String params) {
        super(signature, params);
        int distance = Integer.parseInt(params);
        IDSOption option = signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set distance for sig: " + signature);
            return;	
        }

        ContentOption content = (ContentOption) option;
        content.setDistance(distance);
    }
}
