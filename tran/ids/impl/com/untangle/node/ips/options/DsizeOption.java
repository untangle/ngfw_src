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

package com.untangle.node.ips.options;

import com.untangle.uvm.tapi.event.*;
import com.untangle.node.ips.IPSRuleSignature;
import com.untangle.node.ips.IPSSessionInfo;
import org.apache.log4j.Logger;

public class DsizeOption extends IPSOption {

    private final Logger log = Logger.getLogger(getClass());

    int min;
    int max;
    public DsizeOption(IPSRuleSignature signature, String params) {
        super(signature, params);
        char ch = params.charAt(0);
        String range[] = params.split("<>");
        try {
            if(range.length == 2) {
                min = Integer.parseInt(range[0].trim());
                max = Integer.parseInt(range[1].trim());
            }
            else if(ch == '<') {
                min = 0;
                max = Integer.parseInt(params.substring(1).trim());
            }
            else if(ch == '>') {
                min = Integer.parseInt(params.substring(1).trim());
                max = Integer.MAX_VALUE;
            }
            else
                min = max = Integer.parseInt(params.trim());
        }
        catch(NumberFormatException e) {
            log.error("Invalid Dsize param: " + params);
            min = 0;
            max = Integer.MAX_VALUE;
        }
    }

    public boolean runnable() {
        return true;
    }

    //XXX - check negation flag?
    public boolean run(IPSSessionInfo sessionInfo) {
        IPDataEvent event = sessionInfo.getEvent();
        int size = event.data().remaining();
        if(min <= size && max >= size)
            return true;
        return false;
    }
}
