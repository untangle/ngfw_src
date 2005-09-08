/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.policy;

import java.io.Serializable;

/**
 * Policy.  This is the new top of the world, settings wise.
 *
 * @author
 * @version
 * @hibernate.class
 * table="POLICY"
 */
public class Policy implements Serializable
{
    private static final long serialVersionUID = 6722526125093951941L;

    public static final String NO_NOTES = "no description";

    private Long id;
    private boolean isDefault;
    private String name;
    private String notes = NO_NOTES;

    /**
     * Hibernate constructor.
     */
    public Policy() {}

    Policy(boolean isDefault, String name, String notes)
    {
        this.isDefault = isDefault;
        this.name = name;
        this.notes = notes;
    }

    /**
     * @hibernate.id
     * column="ID"
     * not-null="true"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Returns true if this policy is the default policy.  The default policy is the one selected
     * when a new interface is added.
     *
     * @return true for the default policy
     * @hibernate.property
     * column="IS_DEFAULT"
     * not-null="true"
     */
    public boolean getDefault()
    {
        return isDefault;
    }

    public boolean isDefault()
    {
        return isDefault;
    }

    public void setDefault(boolean isDefault)
    {
        this.isDefault = isDefault;
    }

    /**
     * The name of the policy.  This is a short name used in the UI main policy selector.
     *
     * @hibernate.property
     * not-null="true"
     * column="NAME"
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
     * @hibernate.property
     * column="NOTES"
     */
    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }
}
