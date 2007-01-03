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

package com.untangle.tran.spyware;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.IPMaddrRule;
import com.untangle.mvvm.tran.StringRule;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * Settings for the Spyware transform.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_spyware_settings", schema="settings")
public class SpywareSettings implements Serializable
{
    private static final long serialVersionUID = -2701816808325279608L;

    private Long id;
    private Tid tid;
    private UserWhitelistMode userWhitelistMode = UserWhitelistMode.USER_ONLY;
    private boolean activeXEnabled = true;
    private boolean cookieBlockerEnabled = true;
    private boolean spywareEnabled = true;
    private boolean blockAllActiveX = false;
    private boolean urlBlacklistEnabled = true;
    private String activeXDetails ="no description";
    private String cookieBlockerDetails = "no description";
    private String spywareDetails = "no description";
    private String blockAllActiveXDetails = "no description";
    private String urlBlacklistDetails = "no description";
    private List<StringRule> activeXRules;
    private List<StringRule> cookieRules;
    private List<IPMaddrRule> subnetRules;
    private List<StringRule> domainWhitelist = new ArrayList<StringRule>();

    // not for the GUI! XXX move to a private class
    private int accessVersion = -1;
    private int activeXVersion = -1;
    private int cookieVersion = -1;

    // constructors -----------------------------------------------------------

    public SpywareSettings() { }

    public SpywareSettings(Tid tid)
    {
        this.tid = tid;
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
     * Transform id for these settings.
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

    @Enumerated(EnumType.STRING)
    @Column(name="user_whitelist_mode", nullable=false)
    public UserWhitelistMode getUserWhitelistMode()
    {
        return userWhitelistMode;
    }

    public void setUserWhitelistMode(UserWhitelistMode userWhitelistMode)
    {
        this.userWhitelistMode = userWhitelistMode;
    }

    /**
     * ActiveX scanner enabled.
     *
     * @return true if active.
     */
    @Column(name="activex_enabled", nullable=false)
    public boolean getActiveXEnabled()
    {
        return activeXEnabled;
    }

    public void setActiveXEnabled(boolean activeXEnabled)
    {
        this.activeXEnabled = activeXEnabled;
    }

    /**
     * Cookie blocker enabled.
     *
     * @return true if cookie is enabled.
     */
    @Column(name="cookie_enabled", nullable=false)
    public boolean getCookieBlockerEnabled()
    {
        return cookieBlockerEnabled;
    }

    public void setCookieBlockerEnabled(boolean cookieBlockerEnabled)
    {
        this.cookieBlockerEnabled = cookieBlockerEnabled;
    }

    /**
     * Spyware enabled.
     *
     * @return true if spyware checking enabled.
     */
    @Column(name="spyware_enabled", nullable=false)
    public boolean getSpywareEnabled()
    {
        return spywareEnabled;
    }

    public void setSpywareEnabled(boolean spywareEnabled)
    {
        this.spywareEnabled = spywareEnabled;
    }

    /**
     * All ActiveX blocked.
     *
     * @return true if all ActiveX should be blocked.
     */
    @Column(name="block_all_activex", nullable=false)
    public boolean getBlockAllActiveX()
    {
        return blockAllActiveX;
    }

    public void setBlockAllActiveX(boolean blockAllActiveX)
    {
        this.blockAllActiveX = blockAllActiveX;
    }

    /**
     * Enables the URL blacklist.
     *
     * @return true if blacklist enabled, false otherwise.
     */
    @Column(name="url_blacklist_enabled", nullable=false)
    public boolean getUrlBlacklistEnabled()
    {
        return urlBlacklistEnabled;
    }

    public void setUrlBlacklistEnabled(boolean urlBlacklistEnabled)
    {
        this.urlBlacklistEnabled = urlBlacklistEnabled;
    }

    @Column(name="activex_details")
    public String getActiveXDetails()
    {
        return activeXDetails;
    }

    public void setActiveXDetails(String activeXDetails)
    {
        this.activeXDetails = activeXDetails;
    }

    @Column(name="cookie_details")
    public String getCookieBlockerDetails()
    {
        return cookieBlockerDetails;
    }

    public void setCookieBlockerDetails(String cookieBlockerDetails)
    {
        this.cookieBlockerDetails = cookieBlockerDetails;
    }

    @Column(name="spyware_details")
    public String getSpywareDetails()
    {
        return spywareDetails;
    }

    public void setSpywareDetails(String spywareDetails)
    {
        this.spywareDetails = spywareDetails;
    }

    @Column(name="block_all_activex_details")
    public String getBlockAllActiveXDetails()
    {
        return blockAllActiveXDetails;
    }

    public void setBlockAllActiveXDetails(String blockAllActiveXDetails)
    {
        this.blockAllActiveXDetails = blockAllActiveXDetails;
    }

    @Column(name="url_blacklist_details")
    public String getUrlBlacklistDetails()
    {
        return urlBlacklistDetails;
    }

    public void setUrlBlacklistDetails(String urlBlacklistDetails)
    {
        this.urlBlacklistDetails = urlBlacklistDetails;
    }

    /**
     * ActiveX rules.
     *
     * @return the list of ActiveXRules
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
            org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="tr_spyware_ar",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<StringRule> getActiveXRules()
    {
        return activeXRules;
    }

    public void setActiveXRules(List<StringRule> activeXRules)
    {
        this.activeXRules = activeXRules;
    }

    /**
     * Cookie rules.
     *
     * @return the list of CookieRules.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
            org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="tr_spyware_cr",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<StringRule> getCookieRules()
    {
        return cookieRules;
    }

    public void setCookieRules(List<StringRule> cookieRules)
    {
        this.cookieRules = cookieRules;
    }

    /**
     * IPMaddr rules.
     *
     * @return the list of Subnet Rules.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
            org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="tr_spyware_sr",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<IPMaddrRule> getSubnetRules()
    {
        return subnetRules;
    }

    public void setSubnetRules(List<IPMaddrRule> subnetRules)
    {
        this.subnetRules = subnetRules;
    }

    /**
     * Domains not subject to blacklist checking.
     *
     * @return the list of passed domains.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
               org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="tr_spyware_wl",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<StringRule> getDomainWhitelist()
    {
        return domainWhitelist;
    }

    public void setDomainWhitelist(List<StringRule> domainWhitelist)
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
