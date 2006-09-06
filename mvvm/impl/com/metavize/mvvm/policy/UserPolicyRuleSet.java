/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.policy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection of UserPolicyRules (currently only one row in this table)
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="MVVM_USER_POLICY_RULES"
 */
public class UserPolicyRuleSet implements Serializable
{
    private static final long serialVersionUID = 1806394002255614868L;

    private Long id;

    private List rules = new ArrayList();

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public UserPolicyRuleSet() { }

    // business methods ------------------------------------------------------

    void addRule(UserPolicyRule rule)
    {
        rules.add(rule);
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="SET_ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Rules in this set
     *
     * @return the list of rules
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="SET_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.policy.UserPolicyRule"
     */
    public List getRules()
    {
        return rules;
    }

    public void setRules(List rules)
    {
        this.rules = rules;
    }
}
