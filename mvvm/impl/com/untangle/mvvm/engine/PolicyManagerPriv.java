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

package com.untangle.mvvm.engine;

import com.untangle.mvvm.policy.PolicyManager;
import com.untangle.mvvm.policy.SystemPolicyRule;
import com.untangle.mvvm.policy.UserPolicyRule;

public interface PolicyManagerPriv extends PolicyManager
{
    void reconfigure(byte[] interfaces);
    UserPolicyRule[] getUserRules();
    SystemPolicyRule[] getSystemRules();
}
