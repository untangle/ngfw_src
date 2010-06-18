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
 * A sorted list of IPMaddrRules, used as a directory
 *
 * @author
 * @version 1.0
 */
@Entity
@Table(name="u_ipmaddr_dir", schema="settings")
@SuppressWarnings("serial")
public class IPMaddrDirectory implements Serializable
{

    private Long id;
    private List<IPMaddrRule> entries;
    private String notes;

    public IPMaddrDirectory() {
        entries = new ArrayList<IPMaddrRule>();
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
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
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
    @JoinTable(name="u_ipmaddr_dir_entries",
               joinColumns=@JoinColumn(name="ipmaddr_dir_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<IPMaddrRule> getEntries()
    {
        if (entries != null) entries.removeAll(java.util.Collections.singleton(null));
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
