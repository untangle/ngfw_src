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

package com.untangle.tran.ids;

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

import com.untangle.mvvm.security.Tid;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * Hibernate object to store IDS settings.
 *
 * @author <a href="mailto:nchilders@untangle.com">Nick Childers</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_ids_settings", schema="settings")
public class IDSSettings implements Serializable {
    private static final long serialVersionUID = -7056565971726289302L;
    private int maxChunks;
    private Long id;
    private Tid tid;
    private List<IDSRule> rules = new ArrayList<IDSRule>();
    private List<IDSVariable> variables = new ArrayList<IDSVariable>();
    private List<IDSVariable> immutableVariables = new ArrayList<IDSVariable>();

    public IDSSettings() {}

    public IDSSettings(Tid tid) {
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
     * Transform id for these settings.
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
    public List<IDSRule> getRules() { return this.rules; }
    public void setRules(List<IDSRule> rules) { this.rules = rules; }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="tr_ids_mutable_variables",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="variable_id"))
    @IndexColumn(name="position")
    public List<IDSVariable> getVariables() { return this.variables; }
    public void setVariables(List<IDSVariable> variables) { this.variables = variables; }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="tr_ids_immutable_variables",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="variable_id"))
    @IndexColumn(name="position")
    public List<IDSVariable> getImmutableVariables() { return this.immutableVariables; }
    public void setImmutableVariables(List<IDSVariable> variables) { this.immutableVariables = variables; }
}
