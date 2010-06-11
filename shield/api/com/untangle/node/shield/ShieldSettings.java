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

package com.untangle.node.shield;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.untangle.uvm.security.Tid;

/**
 * Settings for the Shield Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_shield_settings", schema="settings")
public class ShieldSettings implements Serializable
{

    private Long id;
    private Tid tid;

    private ShieldBaseSettings baseSettings = new ShieldBaseSettings();

    private Set<ShieldNodeRule> shieldNodeRules = new LinkedHashSet<ShieldNodeRule>();

    public ShieldSettings() { }

    public ShieldSettings(Tid tid)
    {
        this.tid = tid;
    }

    @Id
    @Column(name="settings_id")
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
     * Node id for these settings.
     *
     * @return tid for these settings
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Shield node configuration rules.
     *
     * @return the set of user settings
     */
    @OneToMany(targetEntity=ShieldNodeRule.class, cascade=CascadeType.ALL,
               fetch=FetchType.EAGER)
    @JoinColumn(name="settings_id")
    public Set<ShieldNodeRule> getShieldNodeRules()
    {
        if (null == this.shieldNodeRules) {
            this.shieldNodeRules = new LinkedHashSet<ShieldNodeRule>();
        }

        return this.shieldNodeRules;
    }

    public void setShieldNodeRules(Set<ShieldNodeRule> shieldNodeRules)
    {
        if (null == shieldNodeRules) {
            shieldNodeRules = new LinkedHashSet<ShieldNodeRule>();
        }

        this.shieldNodeRules = shieldNodeRules;
    }

    @Embedded
    public ShieldBaseSettings getBaseSettings() {
        if (null != baseSettings) {
            baseSettings.setShieldNodeRulesLength(null == shieldNodeRules ? 0 : shieldNodeRules.size());
        }

        return baseSettings;
    }

    public void setBaseSettings(ShieldBaseSettings baseSettings) {
        if (null == baseSettings) {
            baseSettings = new ShieldBaseSettings();
        }
        this.baseSettings = baseSettings;
    }
}
