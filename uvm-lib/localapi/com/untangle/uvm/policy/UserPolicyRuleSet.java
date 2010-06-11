/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.policy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

import com.untangle.node.util.UvmUtil;

/**
 * A collection of UserPolicyRules (currently only one row in this
 * table).
 *
 * @author
 * @version 1.0
 */
@Entity
@Table(name="u_user_policy_rules", schema="settings")
@SuppressWarnings("serial")
public class UserPolicyRuleSet implements Serializable
{

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
        return UvmUtil.eliminateNulls(rules);
    }

    public void setRules(List<UserPolicyRule> rules)
    {
        this.rules = rules;
    }
}
