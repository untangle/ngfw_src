/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ids.options;

import java.nio.ByteBuffer;
import org.apache.log4j.Logger;

import com.metavize.tran.ids.IDSDetectionEngine;
import com.metavize.tran.ids.IDSRule;
import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.tran.ids.IDSTransformImpl;
import com.metavize.tran.ids.RuleClassification;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.event.*;


public class SidOption extends IDSOption {

    private static final Logger logger = Logger.getLogger(SidOption.class);

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
