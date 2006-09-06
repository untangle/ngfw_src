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

package com.metavize.mvvm.tran;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A sorted list of IPMaddrRules, used as a directory
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="IPMADDR_DIR"
 */
public class IPMaddrDirectory implements Serializable
{
    private static final long serialVersionUID = -2636950101710654253L;

    private Long id;
    private List entries;
    private String notes;

    public IPMaddrDirectory() {
        entries = new ArrayList();
    }

    /**
     * Use this to add an entry to the end of the list.
     */
    public void addEntry(IPMaddrRule entry) {
        entries.add(entry);
    }

    /**
     * @hibernate.id
     * column="ID"
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
     * The sorted list of IPMaddrRules -- a client IP matches against
     * each in turn, first one to match is the winner.
     *
     * @return dictionary entries
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="IPMADDR_DIR_ENTRIES"
     * @hibernate.collection-key
     * column="IPMADDR_DIR_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.IPMaddrRule"
     * column="RULE_ID"
     */
    public List getEntries()
    {
        return entries;
    }

    public void setEntries(List entries)
    {
        this.entries = entries;
    }

    /**
     * Notes about the directory
     *
     * @return notes about the directory
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

    // EQUALS? XXXX
}
