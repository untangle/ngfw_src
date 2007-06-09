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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * A collection of UserPolicyRules (currently only one row in this
 * table).
 *
 * @author
 * @version 1.0
 */
@Entity
@Table(name="mvvm_user_policy_rules", schema="settings")
public class UserPolicyRuleSet implements Serializable
{
    private static final long serialVersionUID = 1806394002255614868L;

    private Long id;

    private List<UserPolicyRule> rules = new ArrayList<UserPolicyRule>();

    // constructors -----------------------------------------------------------

    public UserPolicyRuleSet() { }

    // business methods ------------------------------------------------------

    void addRule(UserPolicyRule rule)
    {
        rules.add(rule);
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="set_id")
    @GeneratedValue
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
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="set_id")
    @IndexColumn(name="position")
    public List<UserPolicyRule> getRules()
    {
        return rules;
    }

    public void setRules(List<UserPolicyRule> rules)
    {
        this.rules = rules;
    }
}
