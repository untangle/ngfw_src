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

public class OffsetOption extends IDSOption {

    private final Logger logger = Logger.getLogger(getClass());

    public OffsetOption(IDSRuleSignature signature, String params) throws ParseException {
        super(signature, params);
        ContentOption option = (ContentOption) signature.getOption("ContentOption",this);
        if(option == null) {
            logger.warn("Unable to find content option to set offset for sig: " + signature);
            return;
        }

        int offset = 0;
        try {
            offset = Integer.parseInt(params);
        } catch (Exception e) {
            throw new ParseException("Not a valid Offset argument: " + params);
        }
        option.setOffset(offset);
    }
}
