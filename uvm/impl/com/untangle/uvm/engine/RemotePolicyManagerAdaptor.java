/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: RemoteNodeManagerImpl.java 8600 2007-01-16 02:16:28Z amread $
 */

package com.untangle.uvm.engine;

import com.untangle.uvm.policy.LocalPolicyManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.policy.PolicyConfiguration;
import com.untangle.uvm.policy.PolicyException;
import com.untangle.uvm.policy.PolicyManager;
import com.untangle.uvm.policy.SystemPolicyRule;
import com.untangle.uvm.policy.UserPolicyRule;
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

    public String productIdentifier()
    {
        return pm.productIdentifier();
    }
}
