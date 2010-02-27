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

package com.untangle.node.firewall;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

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

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

import com.untangle.node.util.UvmUtil;
import com.untangle.uvm.security.Tid;

/**
 * Settings for the Firewall node.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="n_firewall_settings", schema="settings")
public class FirewallSettings implements Serializable
{
    /* XXX Must be updated */
    private static final long serialVersionUID = 1629094295874759581L;

    private Long id;
    private Tid tid;

    private FirewallBaseSettings baseSettings = new FirewallBaseSettings();

    private List<FirewallRule> firewallRuleList = null;

    @SuppressWarnings("unused")
    private FirewallSettings() {}

    public FirewallSettings(Tid tid)
    {
        this.tid = tid;
        this.firewallRuleList = new LinkedList();
    }

    @SuppressWarnings("unused")
    @Id
    @Column(name="settings_id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    @SuppressWarnings("unused")
    private void setId( Long id )
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

    public void setTid( Tid tid )
    {
        this.tid = tid;
    }

    @Embedded
    public FirewallBaseSettings getBaseSettings()
    {
        if (null != baseSettings) {
            baseSettings.setFirewallRulesLengh(null == firewallRuleList ? 0 : firewallRuleList.size());
        }

        return baseSettings;
    }

    public void setBaseSettings(FirewallBaseSettings baseSettings)
    {
        if (null == baseSettings) {
            baseSettings = new FirewallBaseSettings();
        }
        this.baseSettings = baseSettings;
    }

    /**
     * List of the redirect rules.
     *
     * @return the list of the redirect rules.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<FirewallRule> getFirewallRuleList()
    {
        return UvmUtil.eliminateNulls(firewallRuleList);
    }

    public void setFirewallRuleList(List<FirewallRule> s)
    {
        this.firewallRuleList = s;
    }
}
