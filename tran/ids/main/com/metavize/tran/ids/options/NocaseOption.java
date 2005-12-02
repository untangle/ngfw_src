package com.metavize.tran.ids.options;
import org.apache.log4j.Logger;

import com.metavize.tran.ids.IDSRuleSignature;

public class NocaseOption extends IDSOption {

    private static final Logger logger = Logger.getLogger(NocaseOption.class);
				
    public NocaseOption(IDSRuleSignature signature, String params) {
        super(signature, params);
        String[] parents = new String [] { "ContentOption", "UricontentOption" };
        IDSOption option = signature.getOption(parents, this);
        if(option == null) {
            logger.warn("Unable to find content option to set nocase for sig: " + signature.rule().getText());
            return;	
        }

        if (option instanceof ContentOption) {
            ContentOption content = (ContentOption) option;
            content.setNoCase();
        } else if (option instanceof UricontentOption) {
            UricontentOption uricontent = (UricontentOption) option;
            uricontent.setNoCase();
        }
    }
}
