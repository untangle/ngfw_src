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

public class DistanceOption extends IDSOption {

    private final Logger logger = Logger.getLogger(getClass());

    public DistanceOption(IDSRuleSignature signature, String params) {
        super(signature, params);
        int distance = Integer.parseInt(params);
        IDSOption option = signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set distance for sig: " + signature);
            return;
        }

        ContentOption content = (ContentOption) option;
        content.setDistance(distance);
    }
}
