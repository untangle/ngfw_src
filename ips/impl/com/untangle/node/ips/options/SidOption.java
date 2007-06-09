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

import com.untangle.node.ips.IPSRule;
import com.untangle.node.ips.IPSRuleSignature;
import com.untangle.uvm.tapi.event.*;
import com.untangle.uvm.tapi.event.*;
import org.apache.log4j.Logger;


public class SidOption extends IPSOption {

    private final Logger logger = Logger.getLogger(getClass());

    public SidOption(IPSRuleSignature signature, String params, boolean initializeSettingsTime) {
        super(signature, params);
        if (initializeSettingsTime) {
            int sid = -1;
            try {
                sid = Integer.parseInt(params);
            } catch (NumberFormatException x) {
                logger.warn("Unable to parse sid: " + params);
            }
            IPSRule rule = signature.rule();
            rule.setSid(sid);
        }
    }
}
