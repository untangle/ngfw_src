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

package com.untangle.uvm.argon;

import java.util.List;

import com.untangle.uvm.policy.PolicyRule;

// XXX merge this class into pipeline and use package protected
// methods when i move impl classes to engine

public class PipelineDesc
{
    private final PolicyRule policyRule;
    private final List<ArgonAgent> agents;

    public PipelineDesc(PolicyRule policyRule, List<ArgonAgent> agents)
    {
        this.policyRule = policyRule;
        this.agents = agents;
    }

    public PolicyRule getPolicyRule()
    {
        return policyRule;
    }

    public List<ArgonAgent> getAgents()
    {
        return agents;
    }
}
