/**
 * $Id$
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
