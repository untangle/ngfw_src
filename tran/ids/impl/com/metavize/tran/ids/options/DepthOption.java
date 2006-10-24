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

import java.util.regex.*;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.tran.ids.IDSRuleSignature;
import org.apache.log4j.Logger;

public class DepthOption extends IDSOption {

    private final Logger logger = Logger.getLogger(getClass());

    public DepthOption(IDSRuleSignature signature, String params) throws ParseException {
        super(signature, params);
        ContentOption option = (ContentOption) signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set depth for sig: " + signature.rule().getText());
            signature.remove(true);
            return;
        }

        int depth = 0;
        try {
            depth = Integer.parseInt(params);
        } catch (Exception e) {
            throw new ParseException("Not a valid Offset argument: " + params);
        }
        option.setDepth(depth);
    }
}
