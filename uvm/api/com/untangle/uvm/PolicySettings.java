/**
 * $Id: Policy.java 31312 2012-03-02 02:07:49Z dmorris $
 */
package com.untangle.uvm.policy;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The settings for a given policy. or Rack.
 */
@SuppressWarnings("serial")
public class PolicySettings implements Serializable
{
    private Long id;
    private boolean isDefault;
    private String name;
    private String notes = "";
    private Long parentId = null;

    // Constructors -----------------------------------------------------------

    public PolicySettings() { }

    // Internal use only
    public PolicySettings(boolean isDefault, String name, String notes)
    {
        this.isDefault = isDefault;
        this.name = name;
        this.notes = notes;
    }

    // UI uses this one.
    public PolicySettings(String name, String notes)
    {
        this.isDefault = false;
        this.name = name;
        this.notes = notes;
    }

    public PolicySettings(String name)
    {
        this.isDefault = false;
        this.name = name;
        this.notes = "";
    }

    // accessors --------------------------------------------------------------

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
     */
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


    /**
     * Returns the ID for the parent, or null if there is no parent
     */
    public Long getParentId()
    {
        return parentId;
    }

    /**
     * Returns the ID for the parent, or null if there is no parent
     */
    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof PolicySettings)) {
            return false;
        } else {

            PolicySettings p = (PolicySettings)o;

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
        return "PolicySettings(" + (isDefault ? "default" : "non-default")
            + ": " + name + " id:" + id + " parentId: " + parentId + ")";
    }
}
