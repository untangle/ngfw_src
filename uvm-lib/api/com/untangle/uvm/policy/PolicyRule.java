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

import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.InterfaceComparator;
import com.untangle.uvm.node.Rule;

/**
 * Hibernate mappings for this class are in the subclasses
 * UserPolicyRule.
 */
@MappedSuperclass
public abstract class PolicyRule extends Rule
{
    /* settings */
    protected Policy policy;

    // constructors -----------------------------------------------------------

    protected PolicyRule() { }

    protected PolicyRule(boolean live, Policy policy)
    {
        super(live);
        this.policy = policy;
    }

    // abstract methods -------------------------------------------------------

    public abstract boolean matches(IPSessionDesc sessionDesc,
                                    InterfaceComparator c);

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
}
