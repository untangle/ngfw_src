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

package com.untangle.node.spyware;

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

import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.security.NodeId;
import org.hibernate.annotations.Cascade;

/**
 * Settings for the Spyware node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_spyware_settings", schema="settings")
@SuppressWarnings("serial")
public class SpywareSettings implements Serializable
{

    private Long id;
    private NodeId tid;

    private SpywareBaseSettings baseSettings = new SpywareBaseSettings();

    private Set<StringRule> activeXRules;
    private Set<StringRule> cookieRules;
    private Set<IPMaddrRule> subnetRules;
    private Set<StringRule> domainWhitelist = new HashSet<StringRule>();

    private int accessVersion = -1;
    private int activeXVersion = -1;
    private int cookieVersion = -1;

    // constructors -------------------------------------------------------

    public SpywareSettings() { }

    public SpywareSettings(NodeId tid)
    {
        this.tid = tid;
    }

    // accessors ----------------------------------------------------------

    @SuppressWarnings("unused")
	@Id
    @Column(name="settings_id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    @SuppressWarnings("unused")
	private void setId(Long id)
    {
        this.id = id;
    }

    @Embedded
    public SpywareBaseSettings getBaseSettings()
    {
        if (null != baseSettings) {
            baseSettings.setActiveXRulesLength(null == activeXRules ? 0 : activeXRules.size());
            baseSettings.setCookieRulesLength(null == cookieRules ? 0 : cookieRules.size());
            baseSettings.setSubnetRulesLength(null == subnetRules ? 0 : subnetRules.size());
            baseSettings.setDomainWhitelistLength(null == domainWhitelist ? 0 : domainWhitelist.size());
        }

        return baseSettings;
    }

    public void setBaseSettings(SpywareBaseSettings baseSettings)
    {
        this.baseSettings = baseSettings;
    }

    /**
     * Node id for these settings.
     *
     * @return tid for these settings
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public NodeId getTid()
    {
        return tid;
    }

    public void setTid(NodeId tid)
    {
        this.tid = tid;
    }

    /**
     * ActiveX rules.
     *
     * @return the set of ActiveXRules
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_spyware_ar",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    public Set<StringRule> getActiveXRules()
    {
        return activeXRules;
    }

    public void setActiveXRules(Set<StringRule> activeXRules)
    {
        this.activeXRules = activeXRules;
    }

    /**
     * Cookie rules.
     *
     * @return the set of CookieRules.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_spyware_cr",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    public Set<StringRule> getCookieRules()
    {
        return cookieRules;
    }

    public void setCookieRules(Set<StringRule> cookieRules)
    {
        this.cookieRules = cookieRules;
    }

    /**
     * IPMaddr rules.
     *
     * @return the set of Subnet Rules.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_spyware_sr",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    public Set<IPMaddrRule> getSubnetRules()
    {
        return subnetRules;
    }

    public void setSubnetRules(Set<IPMaddrRule> subnetRules)
    {
        this.subnetRules = subnetRules;
    }

    /**
     * Domains not subject to blacklist checking.
     *
     * @return the set of passed domains.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_spyware_wl",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    public Set<StringRule> getDomainWhitelist()
    {
        return domainWhitelist;
    }

    public void setDomainWhitelist(Set<StringRule> domainWhitelist)
    {
        this.domainWhitelist = domainWhitelist;
    }

    // NOT FOR THE GUI! XXX move to another class -----------------------------

    /**
     * Current version of subnet list.
     *
     * @return -1 for not initialized, otherwise latest version number.
     */
    @Column(name="subnet_version", nullable=false)
    public int getSubnetVersion()
    {
        return accessVersion;
    }

    public void setSubnetVersion(int accessVersion)
    {
        this.accessVersion = accessVersion;
    }

    /**
     * Current version of ActiveX list.
     *
     * @return -1 for not initialized, otherwise latest version number.
     */
    @Column(name="activex_version", nullable=false)
    public int getActiveXVersion()
    {
        return activeXVersion;
    }

    public void setActiveXVersion(int activeXVersion)
    {
        this.activeXVersion = activeXVersion;
    }

    /**
     * Current version of Cookie list.
     *
     * @return -1 for not initialized, otherwise latest version number.
     */
    @Column(name="cookie_version", nullable=false)
    public int getCookieVersion()
    {
        return cookieVersion;
    }

    public void setCookieVersion(int cookieVersion)
    {
        this.cookieVersion = cookieVersion;
    }
}
