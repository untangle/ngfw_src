/*
 * $HeadURL$
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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

import com.untangle.uvm.node.NodeSettings;

/**
 * Hibernate object to store Ips settings.
 *
 * @author <a href="mailto:nchilders@untangle.com">Nick Childers</a>
 * @version 1.0
 */
@Entity
@Table(name="n_ips_settings", schema="settings")
@SuppressWarnings("serial")
public class IpsSettings implements Serializable
{

    private Long id;
    private NodeSettings tid;

    private IpsBaseSettings baseSettings = new IpsBaseSettings();

    private Set<IpsRule> rules = new HashSet<IpsRule>();
    private Set<IpsVariable> variables = new HashSet<IpsVariable>();
    private Set<IpsVariable> immutableVariables = new HashSet<IpsVariable>();

    public IpsSettings() {}

    public IpsSettings(NodeSettings tid)
    {
        this.tid = tid;
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    protected Long getID() { return id; }
    protected void setID(Long id) { this.id = id; }

    @Embedded
    public IpsBaseSettings getBaseSettings()
    {
        if (null != baseSettings) {
            baseSettings.setRulesLength(null == rules ? 0 : rules.size());
            baseSettings.setVariablesLength(null == variables ? 0 : variables.size());
            baseSettings.setImmutableVariablesLength(null == immutableVariables ? 0 : immutableVariables.size());
            
            int logging = 0;
            int blocking = 0;
            for( IpsRule rule : rules){
                if(rule.isLive())
                    blocking++;
                if(rule.getLog())
                    logging++;
            }
            baseSettings.setTotalAvailable(null == rules ? 0 : rules.size());
            baseSettings.setTotalBlocking(blocking);
            baseSettings.setTotalLogging(logging);
            
        }

        return baseSettings;
    }

    public void setBaseSettings(IpsBaseSettings baseSettings)
    {
        this.baseSettings = baseSettings;
    }

    /**
     * Node id for these settings.
     *
     * @return tid for these settings.
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public NodeSettings getNodeSettings()
    {
        return tid;
    }

    public void setNodeSettings(NodeSettings tid)
    {
        this.tid = tid;
    }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public Set<IpsRule> getRules()
    {
        return this.rules;
    }

    public void setRules(Set<IpsRule> rules) { this.rules = rules; }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_ips_mutable_variables",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="variable_id"))
    @IndexColumn(name="position")
    public Set<IpsVariable> getVariables()
    {
        return this.variables;
    }

    public void setVariables(Set<IpsVariable> variables)
    {
        this.variables = variables;
    }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_ips_immutable_variables",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="variable_id"))
    @IndexColumn(name="position")
    public Set<IpsVariable> getImmutableVariables()
    {
        return this.immutableVariables;
    }

    public void setImmutableVariables(Set<IpsVariable> variables)
    {
        this.immutableVariables = variables;
    }
}
