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

package com.untangle.uvm.security;

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

import com.untangle.uvm.policy.Policy;

/**
 * Node ID.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="u_tid", schema="settings")
@SuppressWarnings("serial")
public class Tid implements Principal, Serializable, Comparable
{

    private Long id;
    private Policy policy;

    // non persistent property XXX maybe we should collapse this and
    // NodePersistentState and also make a immutable token for
    // tid?
    private String nodeName;

    public Tid()
    {
        nodeName = null;
    }

    public Tid(Long id, Policy policy, String nodeName)
    {
        this.id = id;
        this.policy = policy;
        this.nodeName = nodeName;
    }

    public Tid(Long id)
    {
        this.id = id;
        this.policy = null;
        this.nodeName = null;
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

    public void setId(Long id)
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
    public String getNodeName()
    {
        return nodeName;
    }

    public void setNodeName(String nodeName)
    {
        this.nodeName = nodeName;
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
