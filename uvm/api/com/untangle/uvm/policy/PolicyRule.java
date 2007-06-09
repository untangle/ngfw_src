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

package com.untangle.uvm.policy;

import java.util.*;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.Rule;

/**
 * Hibernate mappings for this class are in the subclasses UserPolicyRule
 * and SystemPolicyRule.
 */
@MappedSuperclass
public abstract class PolicyRule extends Rule
{
    /* settings */
    protected Policy policy;
    protected boolean inbound;

    // constructors -----------------------------------------------------------

    protected PolicyRule() { }

    protected PolicyRule(boolean live, Policy policy, boolean inbound)
    {
        super(live);
        this.policy = policy;
        this.inbound = inbound;
    }

    // abstract methods -------------------------------------------------------

    public abstract boolean matches(IPSessionDesc sessionDesc);

    // accessors --------------------------------------------------------------

    /**
     * Policy to apply for this rule.
     *
     * @return Policy for this rule
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="policy_id")
    public Policy getPolicy()
    {
        return policy;
    }

    public void setPolicy(Policy policy)
    {
        this.policy = policy;
    }

    /**
     * Choose the inbound side of the policy?  If false, choose the
     * outbound side.
     *
     * @return true to use inbound side of policy, false outbound
     */
    @Column(name="is_inbound", nullable=false)
    public boolean isInbound()
    {
        return inbound;
    }

    public void setInbound(boolean inbound)
    {
        this.inbound = inbound;
    }
}
