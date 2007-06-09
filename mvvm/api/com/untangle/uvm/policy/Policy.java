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

package com.untangle.mvvm.policy;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Policy.  This is the new top of the world, settings wise.
 *
 * @author
 * @version
 */
@Entity
@Table(name="policy", schema="settings")
public class Policy implements Serializable
{
    private static final long serialVersionUID = 6722526125093951941L;

    public static final String NO_NOTES = "no description";

    private Long id;
    private boolean isDefault;
    private String name;
    //    private boolean live;
    private String notes = NO_NOTES;

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

    private void setId(Long id)
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

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof Policy)) {
            return false;
        } else {
            Policy p = (Policy)o;
            return id.equals(p.id);
        }
    }

    public int hashCode()
    {
        return id.hashCode();
    }

    public String toString()
    {
        return "Policy(" + (isDefault ? "default" : "non-default")
            + ": " + name + ")";
    }
}
