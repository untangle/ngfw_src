/*
 * $HeadURL$
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

import com.untangle.uvm.vnet.event.*;
import com.untangle.node.ips.IPSRuleSignature;
import com.untangle.node.ips.IPSSessionInfo;

public class FlowOption extends IPSOption {

    /**
     * The options "only_stream" and "established" would  have *no* effect.
     * So I ignore them.
     */

    private static Pattern noStream = Pattern.compile("no_stream",Pattern.CASE_INSENSITIVE);
    private static Pattern[] validParams = {
        Pattern.compile("from_server", Pattern.CASE_INSENSITIVE),
        Pattern.compile("from_client", Pattern.CASE_INSENSITIVE),
        Pattern.compile("to_client", Pattern.CASE_INSENSITIVE),
        Pattern.compile("to_server", Pattern.CASE_INSENSITIVE)
    };

    private boolean matchFromServer = false;

    public FlowOption(IPSRuleSignature signature, String params) {
        super(signature, params);
        if(noStream.matcher(params).find()) {
            signature.remove(true);
        }

        for(int i=0; i < validParams.length; i++) {
            if(validParams[i].matcher(params).find())
                matchFromServer = (i%2 == 0);
        }
    }

    public boolean runnable() {
        return true;
    }

    public boolean run(IPSSessionInfo sessionInfo) {
        boolean fromServer = sessionInfo.isServer();
        boolean returnValue = !(fromServer ^ matchFromServer);
        return (negationFlag ^ returnValue);
    }
}
