/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.pipeline;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.CompoundSettings;
import com.metavize.mvvm.policy.PolicyConfiguration;
import com.metavize.mvvm.IntfEnum;

public class PolicyCompoundSettings implements CompoundSettings {

    // POLICY CONFIGURATION //
    private PolicyConfiguration policyConfiguration;
    public PolicyConfiguration getPolicyConfiguration(){ return policyConfiguration; }

    // INTF ENUM //
    private IntfEnum intfEnum;
    public IntfEnum getIntfEnum(){ return intfEnum; }

    public void save() throws Exception {
	Util.getPolicyManager().setPolicyConfiguration(policyConfiguration);
    }

    public void refresh() throws Exception {
	policyConfiguration = Util.getPolicyManager().getPolicyConfiguration();
	intfEnum = Util.getIntfManager().getIntfEnum();
    }

    public void validate() throws Exception {

    }

}
