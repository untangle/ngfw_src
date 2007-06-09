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

import com.untangle.tran.ids.IDSRuleSignature;
import org.apache.log4j.Logger;

public class WithinOption extends IDSOption {

    private final Logger logger = Logger.getLogger(getClass());

    public WithinOption(IDSRuleSignature signature, String params) {
        super(signature, params);
        int within = Integer.parseInt(params);
        IDSOption option = signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set within for sig: " + signature);
            return;
        }

        ContentOption content = (ContentOption) option;
        content.setWithin(within);
    }
}
