/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.policy;

import java.util.*;

import com.metavize.mvvm.argon.IPSessionDesc;
import com.metavize.mvvm.tran.Rule;

/**
 * Hibernate mappings for this class are in the subclasses UserPolicyRule
 * and SystemPolicyRule.
 */
public abstract class PolicyRule extends Rule
{
    /* settings */
    protected Policy policy;
    protected byte clientIntf;
    protected byte serverIntf;
    protected boolean inbound;

    // constructors -----------------------------------------------------------

    protected PolicyRule() { }

    protected PolicyRule(byte clientIntf, byte serverIntf, Policy policy, boolean inbound) {
        this.clientIntf = clientIntf;
        this.serverIntf = serverIntf;
        this.policy = policy;
        this.inbound = inbound;
    }

    // abstract methods -------------------------------------------------------

    public abstract boolean matches(IPSessionDesc sessionDesc);

    // accessors --------------------------------------------------------------

    /**
     * Returns the client interface
     *
     * @return the interface the client must be on to match this rule
     * @hibernate.property
     * column="CLIENT_INTF"
     * not-null="true"
     */
    public byte getClientIntf()
    {
        return clientIntf;
    }

    public void setClientIntf(byte clientIntf)
    {
        this.clientIntf = clientIntf;
    }

    /**
     * Returns the server interface
     *
     * @return the interface the server must be on to match this rule
     * @hibernate.property
     * column="SERVER_INTF"
     * not-null="true"
     */
    public byte getServerIntf()
    {
        return serverIntf;
    }

    public void setServerIntf(byte serverIntf)
    {
        this.serverIntf = serverIntf;
    }

    /**
     * Policy to apply for this rule.
     *
     * @return Policy for this rule
     * @hibernate.many-to-one
     * column="POLICY_ID"
     */
    public Policy getPolicy()
    {
        return policy;
    }

    public void setPolicy(Policy policy)
    {
        this.policy = policy;
    }

    /**
     * Choose the inbound side of the policy?  If false, choose the outbound side.
     *
     * @return true to use inbound side of policy, false outbound
     * @hibernate.property
     * column="IS_INBOUND"
     * not-null="true"
     */
    public boolean isInbound()
    {
        return inbound;
    }

    public void setInbound(boolean inbound)
    {
        this.inbound = inbound;
    }

}
