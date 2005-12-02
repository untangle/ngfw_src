package com.metavize.tran.ids.options;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.BMPattern;
import java.util.regex.*;
import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.tran.ids.IDSSessionInfo;

public class UricontentOption extends IDSOption {

    private BMPattern uriPattern;
    private String stringPattern;

    public UricontentOption(IDSRuleSignature signature, String params) {
        super(signature, params);
        stringPattern = params;
        uriPattern = new BMPattern(stringPattern, false);
    }

    public void setNoCase() {
        uriPattern = new BMPattern(stringPattern, true);
    }

    public boolean runnable() {
        return true;
    }

    public boolean run(IDSSessionInfo sessionInfo) {
        String path = sessionInfo.getUriPath();
        if(path != null) {
            int result = uriPattern.matches(path, 0, path.length());
            return negationFlag ^ (result >= 0);
        }
        return false;
    }
}
