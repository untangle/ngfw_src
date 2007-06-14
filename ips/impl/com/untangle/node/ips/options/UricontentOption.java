/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips.options;

import java.util.regex.*;

import com.untangle.node.ips.IPSRuleSignature;
import com.untangle.node.ips.IPSSessionInfo;
import com.sun.org.apache.xerces.internal.impl.xpath.regex.BMPattern;

public class UricontentOption extends IPSOption {

    private BMPattern uriPattern;
    private String stringPattern;

    public UricontentOption(IPSRuleSignature signature, String params) {
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

    public boolean run(IPSSessionInfo sessionInfo) {
        String path = sessionInfo.getUriPath();
        if(path != null) {
            int result = uriPattern.matches(path, 0, path.length());
            return negationFlag ^ (result >= 0);
        }
        return false;
    }
}
