/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
 * A sorted list of IPMaskedAddressRules, used as a directory
 *
 * @author
 * @version 1.0
 */
@Entity
@Table(name="u_ipmaddr_dir", schema="settings")
@SuppressWarnings("serial")
public class IPMaskedAddressDirectory implements Serializable
{

    private Long id;
    private List<IPMaskedAddressRule> entries;
    private String notes;

    public IPMaskedAddressDirectory() {
        entries = new ArrayList<IPMaskedAddressRule>();
    }

    /**
     * Use this to add an entry to the end of the list.
     */
    public void addEntry(IPMaskedAddressRule entry) {
        entries.add(entry);
    }

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
     * The sorted list of IPMaskedAddressRules -- a client IP matches against
     * each in turn, first one to match is the winner.
     *
     * @return dictionary entries
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="u_ipmaddr_dir_entries",
               joinColumns=@JoinColumn(name="ipmaddr_dir_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<IPMaskedAddressRule> getEntries()
    {
        if (entries != null) entries.removeAll(java.util.Collections.singleton(null));
        return entries;
    }

    public void setEntries(List<IPMaskedAddressRule> entries)
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
