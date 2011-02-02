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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Policy.
 *
 * @author Aaron Read <amread@untangle.com>
 */
@Entity
@Table(name="u_policy", schema="settings")
@SuppressWarnings("serial")
public class Policy implements Serializable
{

    public static final String NO_NOTES = "no description";

    private Long id;
    private boolean isDefault;
    private String name;
    //    private boolean live;
    private String notes = NO_NOTES;

    private Long parentId = null;

    // Constructors -----------------------------------------------------------

    public Policy() { }

    // Internal use only
    public Policy(boolean isDefault, String name, String notes)
    {
        this.isDefault = isDefault;
        this.name = name;
        this.notes = notes;
    }

    // UI uses this one.
    public Policy(String name, String notes)
    {
        this.isDefault = false;
        this.name = name;
        this.notes = notes;
    }

    public Policy(String name)
    {
        this.isDefault = false;
        this.name = name;
        this.notes = NO_NOTES;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="id")
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Returns true if this policy is the default policy.  The default
     * policy is the one selected when a new interface is added.
     *
     * @return true for the default policy
     */
    @Column(name="is_default", nullable=false)
    public boolean isDefault()
    {
        return isDefault;
    }

    public void setDefault(boolean isDefault)
    {
        this.isDefault = isDefault;
    }

    /**
     * The name of the policy.  This is a short name used in the UI
     * main policy selector.
     */
    @Column(nullable=false)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * notes: a string containing notes (defaults to NO_NOTES)
     *
     * @return the notes for this policy
     */
    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    @Column(name="parent_id")
    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
        throws PolicyException
    {
        this.parentId = parentId;
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof Policy)) {
            return false;
        } else {

            Policy p = (Policy)o;

            /* rbscott did this, and rbscott thinks it is bad.*/
//             if (id == null) {
//                 if (p.id == null) {
//                     return name.equals(p.name) && notes.equals(p.notes);
//                 } else {
//                     return false;
//                 }
//             }

            if (p == null) {
                return false;
            } else {
                return id == null ? id == p.id : id.equals(p.id);
            }
        }
    }

    public int hashCode()
    {
        return id.hashCode();
    }

    public String toString()
    {
        return "Policy(" + (isDefault ? "default" : "non-default")
            + ": " + name + " id:" + id + " parentId: " + parentId + ")";
    }
}
