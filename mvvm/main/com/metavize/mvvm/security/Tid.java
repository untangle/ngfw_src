/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.security;

import com.metavize.mvvm.policy.Policy;
import java.io.Serializable;
import java.security.Principal;

/**
 * Transform ID.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TID"
 */
public final class Tid implements Principal, Serializable, Comparable
{
    private static final long serialVersionUID = -5009447155713499894L;

    private Long id;

    private Policy policy;

    public Tid() { }

    public Tid(Long id, Policy policy)
    {
        this.id = id;
        this.policy = policy;
    }

    // Temporary for backwards compatibility.  SHOULD GO AWAY. XXXX
    public Tid(Long id)
    {
        com.metavize.mvvm.policy.PolicyManager pm = com.metavize.mvvm.MvvmContextFactory.context().policyManager();
        Policy p = pm.getDefaultPolicy();
        this.id = id;
        this.policy = p;
    }

    /**
     * The Long representation of this Tid.
     *
     * @return the Tid as a Long.
     * @hibernate.id
     * generator-class="assigned"
     * column="ID"
     */
    public Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Policy that this TID lives in
     *
     * @return Policy for this Tid
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

    // XXX something more appropriate
    public String getName()
    {
        return Long.toString(id);
    }

    // Comparable methods -----------------------------------------------------

    public int compareTo(Object o)
    {
        if (!(o instanceof Tid)) { throw new IllegalArgumentException(); }

        Tid tid = (Tid)o;

        return id < tid.getId() ? -1 : (id > tid.getId() ? 1 : 0);
    }

    // Object methods ---------------------------------------------------------

    // XXX something more appropriate
    public String toString()
    {
        return getName();
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof Tid)) {
            return false;
        }
        Tid t = (Tid)o;

        return id.equals(t.getId());
    }

    public int hashCode()
    {
        return id.hashCode();
    }
}
