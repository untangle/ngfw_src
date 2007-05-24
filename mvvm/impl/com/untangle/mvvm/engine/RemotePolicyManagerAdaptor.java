/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: RemoteTransformManagerImpl.java 8600 2007-01-16 02:16:28Z amread $
 */

package com.untangle.mvvm.engine;

import com.untangle.mvvm.policy.LocalPolicyManager;
import com.untangle.mvvm.policy.Policy;
import com.untangle.mvvm.policy.PolicyConfiguration;
import com.untangle.mvvm.policy.PolicyException;
import com.untangle.mvvm.policy.PolicyManager;
import com.untangle.mvvm.policy.SystemPolicyRule;
import com.untangle.mvvm.policy.UserPolicyRule;
import java.util.List;

class RemotePolicyManagerAdaptor implements PolicyManager
{
    private final LocalPolicyManager pm;

    RemotePolicyManagerAdaptor(LocalPolicyManager pm)
    {
        this.pm = pm;
    }

    public Policy[] getPolicies()
    {
        return pm.getPolicies();
    }

    public Policy getDefaultPolicy()
    {
        return pm.getDefaultPolicy();
    }

    public Policy getPolicy(String name)
    {
        return pm.getPolicy(name);
    }

    public void addPolicy(String name, String notes) throws PolicyException
    {
        pm.addPolicy(name, notes);
    }

    public void removePolicy(Policy policy) throws PolicyException
    {
        pm.removePolicy(policy);
    }

    public void setPolicy(Policy rule, String name, String notes)
        throws PolicyException
    {
        pm.setPolicy(rule, name, notes);
    }

    public SystemPolicyRule[] getSystemPolicyRules()
    {
        return pm.getSystemPolicyRules();
    }

    public void setSystemPolicyRule(SystemPolicyRule rule, Policy p,
                                    boolean inbound, String description)
    {
        pm.setSystemPolicyRule(rule, p, inbound, description);
    }

    public UserPolicyRule[] getUserPolicyRules()
    {
        return pm.getUserPolicyRules();
    }

    public void setUserPolicyRules(List rules)
    {
        pm.setUserPolicyRules(rules);
    }

    public PolicyConfiguration getPolicyConfiguration()
    {
        return pm.getPolicyConfiguration();
    }

    public void setPolicyConfiguration(PolicyConfiguration pc)
        throws PolicyException
    {
        pm.setPolicyConfiguration(pc);
    }
}
