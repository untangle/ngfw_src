/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ids.options;

import com.metavize.tran.ids.IDSRule;
import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tapi.event.*;
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
