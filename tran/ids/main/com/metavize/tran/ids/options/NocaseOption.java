package com.metavize.tran.ids.options;
import org.apache.log4j.Logger;

import com.metavize.tran.ids.IDSRuleSignature;

public class NocaseOption extends IDSOption {

    private static final Logger logger = Logger.getLogger(NocaseOption.class);
				
    public NocaseOption(IDSRuleSignature signature, String params) {
        super(signature, params);
        IDSOption option = signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set nocase for sig: " + signature);
            return;	
        }

        ContentOption content = (ContentOption) option;
        content.setNoCase();
    }
}
