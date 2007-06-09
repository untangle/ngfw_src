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

package com.untangle.uvm.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * A sorted list of IPMaddrRules, used as a directory
 *
 * @author
 * @version 1.0
 */
@Entity
@Table(name="u_ipmaddr_dir", schema="settings")
public class IPMaddrDirectory implements Serializable
{
    private static final long serialVersionUID = -2636950101710654253L;

    private Long id;
    private List<IPMaddrRule> entries;
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

    @Id
    @Column(name="id")
    @GeneratedValue
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
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="ipmaddr_dir_entries",
               joinColumns=@JoinColumn(name="ipmaddr_dir_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<IPMaddrRule> getEntries()
    {
        return entries;
    }

    public void setEntries(List<IPMaddrRule> entries)
    {
        this.entries = entries;
    }

    /**
     * Notes about the directory
     *
     * @return notes about the directory
     */
    @Column(name="notes")
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
