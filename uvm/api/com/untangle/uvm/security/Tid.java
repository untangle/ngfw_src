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

package com.untangle.mvvm.security;

import java.io.Serializable;
import java.security.Principal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.policy.Policy;

/**
 * Transform ID.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="tid", schema="settings")
public class Tid implements Principal, Serializable, Comparable
{
    private static final long serialVersionUID = -3752177143597737103L;

    private Long id;
    private Policy policy;

    // non persistent property XXX maybe we should collapse this and
    // TransformPersistentState and also make a immutable token for
    // tid?
    private String transformName;

    public Tid()
    {
        transformName = null;
    }

    public Tid(Long id, Policy policy, String transformName)
    {
        this.id = id;
        this.policy = policy;
        this.transformName = transformName;
    }

    public Tid(Long id)
    {
        this.id = id;
        this.policy = null;
        this.transformName = null;
    }

    /**
     * The Long representation of this Tid.
     *
     * @return the Tid as a Long.
     */
    @Id
    @Column(name="id")
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

    @Transient
    public String getTransformName()
    {
        return transformName;
    }

    public void setTransformName(String transformName)
    {
        this.transformName = transformName;
    }

    // XXX something more appropriate
    @Transient
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
