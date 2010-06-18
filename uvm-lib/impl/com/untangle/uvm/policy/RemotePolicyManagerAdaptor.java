/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.policy;

import java.util.List;

import com.untangle.uvm.node.Validator;

/**
 * Adapts LocalPolicyManager to RemotePolicyManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class RemotePolicyManagerAdaptor implements RemotePolicyManager
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

    public void addPolicy(String name, String notes)
        throws PolicyException
    {
        pm.addPolicy(name, notes, null);
    }

    public void addPolicy(String name, String notes, Policy parent)
        throws PolicyException
    {
        pm.addPolicy(name, notes, parent);
    }

    public void removePolicy(Policy policy) throws PolicyException
    {
        pm.removePolicy(policy);
    }

    public void setPolicy(Policy rule, String name, String notes, Policy parent)
        throws PolicyException
    {
        pm.setPolicy(rule, name, notes, parent);
    }

    public UserPolicyRule[] getUserPolicyRules()
    {
        return pm.getUserPolicyRules();
    }

    public void setUserPolicyRules(List<UserPolicyRule> rules)
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

    public void shutdownSessions(Policy policy)
    {
        pm.shutdownSessions(policy);
    }

    public Validator getValidator()
    {
        return pm.getValidator();
    }
}
