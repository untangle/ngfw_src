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

package com.untangle.node.webfilter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
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

import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.node.MimeTypeRule;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.security.Tid;

/**
 * WebFilter settings.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_webfilter_settings", schema="settings")
@SuppressWarnings("serial")
public class WebFilterSettings implements Serializable
{

    private Long id;
    private Tid tid;

    private WebFilterBaseSettings baseSettings = new WebFilterBaseSettings();

    private Set<IPMaddrRule> passedClients = new HashSet<IPMaddrRule>();
    private Set<StringRule> passedUrls = new HashSet<StringRule>();

    private Set<StringRule> blockedUrls = new HashSet<StringRule>();
    private Set<MimeTypeRule> blockedMimeTypes = new HashSet<MimeTypeRule>();
    private Set<StringRule> blockedExtensions = new HashSet<StringRule>();
    private Set<BlacklistCategory> blacklistCategories = new HashSet<BlacklistCategory>();

    // constructors -----------------------------------------------------------

    public WebFilterSettings() { }

    public WebFilterSettings(Tid tid)
    {
        this.tid = tid;
    }

    // business methods ------------------------------------------------------

    public void addBlacklistCategory(BlacklistCategory category)
    {
        blacklistCategories.add(category);
    }

    public BlacklistCategory getBlacklistCategory(String name)
    {
        for (Iterator<BlacklistCategory> i = blacklistCategories.iterator(); i.hasNext(); ) {
            BlacklistCategory bc = i.next();
            if (name.equals(bc.getName())) {
                return bc;
            }
        }

        return null;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="settings_id")
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
     * Node id for these settings.
     *
     * @return tid for these settings.
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
     * Clients not subject to blacklisting.
     *
     * @return the set of passed clients.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_webfilter_passed_clients",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    public Set<IPMaddrRule> getPassedClients()
    {
        return passedClients;
    }

    public void setPassedClients(Set<IPMaddrRule> passedClients)
    {
        this.passedClients = passedClients;
    }

    /**
     * URLs not subject to blacklist checking.
     *
     * @return the set of passed URLs.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_webfilter_passed_urls",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    public Set<StringRule> getPassedUrls()
    {
        return passedUrls;
    }

    public void setPassedUrls(Set<StringRule> passedUrls)
    {
        this.passedUrls = passedUrls;
    }

    /**
     * URLs not subject to blacklist checking.
     *
     * @return the set of blocked URLs.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_webfilter_blocked_urls",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    public Set<StringRule> getBlockedUrls()
    {
        return blockedUrls;
    }

    public void setBlockedUrls(Set<StringRule> blockedUrls)
    {
        this.blockedUrls = blockedUrls;
    }

    /**
     * Mime-Types to be blocked.
     *
     * @return the set of blocked MIME types.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_webfilter_mime_types",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    public Set<MimeTypeRule> getBlockedMimeTypes()
    {
        return blockedMimeTypes;
    }

    public void setBlockedMimeTypes(Set<MimeTypeRule> blockedMimeTypes)
    {
        this.blockedMimeTypes = blockedMimeTypes;
    }

    /**
     * Extensions to be blocked.
     *
     * @return the set of blocked extensions.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_webfilter_extensions",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    public Set<StringRule> getBlockedExtensions()
    {
        return blockedExtensions;
    }

    public void setBlockedExtensions(Set<StringRule> blockedExtensions)
    {
        this.blockedExtensions = blockedExtensions;
    }

    /**
     * Blacklist blocking options.
     *
     * @return the set of blacklist categories.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="setting_id")
    public Set<BlacklistCategory> getBlacklistCategories()
    {
        return blacklistCategories;
    }

    public void setBlacklistCategories(Set<BlacklistCategory> blacklistCategories)
    {
        this.blacklistCategories = blacklistCategories;
    }

    @Embedded
    public WebFilterBaseSettings getBaseSettings() {
        if (null != baseSettings) {
            baseSettings.setPassedClientsLength(null == passedClients ? 0 : passedClients.size());
            baseSettings.setPassedUrlsLength(null == passedUrls ? 0 : passedUrls.size());
            baseSettings.setBlockedUrlsLength(null == blockedUrls ? 0 : blockedUrls.size());
            baseSettings.setBlockedMimeTypesLength(null == blockedMimeTypes ? 0 : blockedMimeTypes.size());
            baseSettings.setBlockedExtensionsLength(null == blockedExtensions ? 0 : blockedExtensions.size());
            baseSettings.setBlacklistCategoriesLength(null == blacklistCategories ? 0 : blacklistCategories.size());
        }

        return baseSettings;
    }

    public void setBaseSettings(WebFilterBaseSettings baseSettings) {
        this.baseSettings = baseSettings;
    }
}
