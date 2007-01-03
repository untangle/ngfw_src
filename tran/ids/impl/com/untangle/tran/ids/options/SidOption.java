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

package com.untangle.tran.ids.options;

import com.untangle.tran.ids.IDSRule;
import com.untangle.tran.ids.IDSRuleSignature;
import com.untangle.mvvm.tapi.event.*;
import com.untangle.mvvm.tapi.event.*;
import org.apache.log4j.Logger;


public class SidOption extends IDSOption {

    private final Logger logger = Logger.getLogger(getClass());

    public SidOption(IDSRuleSignature signature, String params, boolean initializeSettingsTime) {
        super(signature, params);
        if (initializeSettingsTime) {
            int sid = -1;
            try {
                sid = Integer.parseInt(params);
            } catch (NumberFormatException x) {
                logger.warn("Unable to parse sid: " + params);
            }
            IDSRule rule = signature.rule();
            rule.setSid(sid);
        }
    }
}
