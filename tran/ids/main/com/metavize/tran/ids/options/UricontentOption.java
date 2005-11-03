package com.metavize.tran.ids.options;

import java.util.regex.*;
import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.tran.ids.IDSSessionInfo;

public class UricontentOption extends IDSOption {

    private Pattern uriPattern;
    public UricontentOption(IDSRuleSignature signature, String params) {
        super(signature, params);
        uriPattern = Pattern.compile(params, Pattern.LITERAL);
    }

    public boolean runnable() {
        return true;
    }
    public boolean run(IDSSessionInfo sessionInfo) {
        String path = sessionInfo.getUriPath();
        if(path != null) {
            return negationFlag ^ uriPattern.matcher(path).find();
        }
        return false;
    }
}
