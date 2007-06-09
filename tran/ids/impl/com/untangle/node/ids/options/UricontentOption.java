/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.ids.options;

import java.util.regex.*;

import com.untangle.node.ids.IDSRuleSignature;
import com.untangle.node.ids.IDSSessionInfo;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.BMPattern;

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
