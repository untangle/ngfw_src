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

import java.util.regex.*;

import com.untangle.node.ips.IPSRuleSignature;

/**
 * This class matches the reference option found in snort based rule
 * signatures.
 *
 * @Author Nick Childers
 */
public class ReferenceOption extends IPSOption {
    private static final Pattern URLP = Pattern.compile("url,", Pattern.CASE_INSENSITIVE);

    public ReferenceOption(IPSRuleSignature signature, String params) {
        super(signature, params);

        Matcher urlm = URLP.matcher(params);
        if (true == urlm.find()) {
            String url = "http://" + params.substring(urlm.end()).trim();
            signature.setURL(url);
        }
    }
}
