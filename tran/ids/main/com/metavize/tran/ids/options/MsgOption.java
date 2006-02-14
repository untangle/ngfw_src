/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ids.options;

import java.util.regex.*;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.ids.IDSRuleSignature;

public class MsgOption extends IDSOption {

    public MsgOption(IDSRuleSignature signature, String params) {
        super(signature, params);
        signature.setMessage(params);
    }
}
