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

package com.untangle.mvvm.policy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PolicyConfiguration implements Serializable
{
    private static final long serialVersionUID = -8235091515969174553L;

    private List policies;

    private List systemPolicyRules;

    private List userPolicyRules;

    public PolicyConfiguration(List policies, SystemPolicyRule[] sysRules, UserPolicyRule[] userRules) {
        this.policies = new ArrayList(policies);
        systemPolicyRules = new ArrayList(sysRules.length);
        for (int i = 0; i < sysRules.length; i++)
            systemPolicyRules.add(sysRules[i]);
        userPolicyRules = new ArrayList(userRules.length);
        for (int i = 0; i < userRules.length; i++)
            userPolicyRules.add(userRules[i]);
    }

    public List getPolicies() {
        return policies;
    }

    public void setPolicies(List policies) {
        this.policies = policies;
    }

    public List getSystemPolicyRules() {
        return systemPolicyRules;
    }

    public void setSystemPolicyRules(List systemPolicyRules) {
        this.systemPolicyRules = systemPolicyRules;
    }

    public List getUserPolicyRules() {
        return userPolicyRules;
    }

    public void setUserPolicyRules(List userPolicyRules) {
        this.userPolicyRules = userPolicyRules;
    }
}

    
