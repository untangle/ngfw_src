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

import com.untangle.mvvm.tapi.event.*;
import com.untangle.tran.ids.IDSDetectionEngine;
import com.untangle.tran.ids.IDSRule;
import com.untangle.tran.ids.IDSRuleSignature;
import com.untangle.tran.ids.RuleClassification;
import org.apache.log4j.Logger;

public class ClasstypeOption extends IDSOption {
    private static final int HIGH_PRIORITY = 1;
    private static final int MEDIUM_PRIORITY = 2;
    private static final int LOW_PRIORITY = 3;
    private static final int INFORMATIONAL_PRIORITY = 4; // Super low priority

    private final Logger logger = Logger.getLogger(getClass());

    public ClasstypeOption(IDSDetectionEngine engine, IDSRuleSignature signature, String params, boolean initializeSettingsTime) {
        super(signature, params);

        RuleClassification rc = null;
        if (engine != null)
            // Allow null for testing.
            rc = engine.getClassification(params);
        if (rc == null) {
            logger.warn("Unable to find rule classification: " + params);
            // use default classification text for signature
        } else {
            signature.setClassification(rc.getDescription());

            if (true == initializeSettingsTime) {
                IDSRule rule = signature.rule();
                int priority = rc.getPriority();
                // logger.debug("Rule Priority for " + rule.getDescription() + " is " + priority);
                switch (priority) {
                case HIGH_PRIORITY:
                    rule.setLive(true);
                    rule.setLog(true);
                    break;
                case MEDIUM_PRIORITY:
                    rule.setLog(true);
                    break;
                default:
                    break;
                }
            }
        }
    }
}
