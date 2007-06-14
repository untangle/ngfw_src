/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips;

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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.untangle.uvm.security.Tid;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * Hibernate object to store IPS settings.
 *
 * @author <a href="mailto:nchilders@untangle.com">Nick Childers</a>
 * @version 1.0
 */
@Entity
@Table(name="n_ips_settings", schema="settings")
public class IPSSettings implements Serializable {
    private static final long serialVersionUID = -7056565971726289302L;
    private int maxChunks;
    private Long id;
    private Tid tid;
    private List<IPSRule> rules = new ArrayList<IPSRule>();
    private List<IPSVariable> variables = new ArrayList<IPSVariable>();
    private List<IPSVariable> immutableVariables = new ArrayList<IPSVariable>();

    public IPSSettings() {}

    public IPSSettings(Tid tid) {
        this.tid = tid;
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    protected Long getID() { return id; }
    protected void setID(Long id) { this.id = id; }

    @Column(name="max_chunks")
    protected int getMaxChunks() { return maxChunks; }
    protected void setMaxChunks(int maxChunks) { this.maxChunks = maxChunks; }

    /**
     * Node id for these settings.
     *
     * @return tid for these settings.
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public Tid getTid() {
        return tid;
    }

    public void setTid(Tid tid) {
        this.tid = tid;
    }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<IPSRule> getRules() { return this.rules; }
    public void setRules(List<IPSRule> rules) { this.rules = rules; }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_ips_mutable_variables",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="variable_id"))
    @IndexColumn(name="position")
    public List<IPSVariable> getVariables() { return this.variables; }
    public void setVariables(List<IPSVariable> variables) { this.variables = variables; }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_ips_immutable_variables",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="variable_id"))
    @IndexColumn(name="position")
    public List<IPSVariable> getImmutableVariables() { return this.immutableVariables; }
    public void setImmutableVariables(List<IPSVariable> variables) { this.immutableVariables = variables; }
}
