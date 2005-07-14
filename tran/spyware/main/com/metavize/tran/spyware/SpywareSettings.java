/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spyware;

import java.io.Serializable;
import java.util.List;

import com.metavize.mvvm.security.Tid;

/**
 * Settings for the Spyware transform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_SPYWARE_SETTINGS"
 */
public class SpywareSettings implements Serializable
{
    private static final long serialVersionUID = -2701816808325279608L;

    private Long id;
    private Tid tid;
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
    private List activeXRules;
    private List cookieRules;
    private List subnetRules;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpywareSettings() { }

    public SpywareSettings(Tid tid)
    {
        this.tid = tid;
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
     * generator-class="native"
     */
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
     * @hibernate.many-to-one
     * column="TID"
     * unique="true"
     * not-null="true"
     */
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * ActiveX scanner enabled.
     *
     * @return true if active.
     * @hibernate.property
     * column="ACTIVEX_ENABLED"
     */
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
     * @hibernate.property
     * column="COOKIE_ENABLED"
     */
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
     * @hibernate.property
     * column="SPYWARE_ENABLED"
     */
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
     * @hibernate.property
     * column="BLOCK_ALL_ACTIVEX"
     */
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
     * @hibernate.property
     * column="URL_BLACKLIST_ENABLED"
     */
    public boolean getUrlBlacklistEnabled()
    {
        return urlBlacklistEnabled;
    }

    public void setUrlBlacklistEnabled(boolean urlBlacklistEnabled)
    {
        this.urlBlacklistEnabled = urlBlacklistEnabled;
    }

    /**
     * XXX what does this do
     *
     * @return XXX
     * @hibernate.property
     * column="ACTIVEX_DETAILS"
     */
    public String getActiveXDetails()
    {
        return activeXDetails;
    }

    public void setActiveXDetails(String activeXDetails)
    {
        this.activeXDetails = activeXDetails;
    }

    /**
     * XXX what does this do
     *
     * @return XXX
     * @hibernate.property
     * column="COOKIE_DETAILS"
     */
    public String getCookieBlockerDetails()
    {
        return cookieBlockerDetails;
    }

    public void setCookieBlockerDetails(String cookieBlockerDetails)
    {
        this.cookieBlockerDetails = cookieBlockerDetails;
    }

    /**
     * XXX what does this do?
     *
     * @return XXX
     * @hibernate.property
     * column="SPYWARE_DETAILS"
     */
    public String getSpywareDetails()
    {
        return spywareDetails;
    }

    public void setSpywareDetails(String spywareDetails)
    {
        this.spywareDetails = spywareDetails;
    }

    /**
     * XXX what does this do?
     *
     * @return XXX
     * @hibernate.property
     * column="BLOCK_ALL_ACTIVEX_DETAILS"
     */
    public String getBlockAllActiveXDetails()
    {
        return blockAllActiveXDetails;
    }

    public void setBlockAllActiveXDetails(String blockAllActiveXDetails)
    {
        this.blockAllActiveXDetails = blockAllActiveXDetails;
    }

    /**
     * XXX what does this do?
     *
     * @return XXX
     * @hibernate.property
     * column="URL_BLACKLIST_DETAILS"
     */
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
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_SPYWARE_AR"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.StringRule"
     * column="RULE_ID"
     */
    public List getActiveXRules()
    {
        return activeXRules;
    }

    public void setActiveXRules(List activeXRules)
    {
        this.activeXRules = activeXRules;
    }

    /**
     * Cookie rules.
     *
     * @return the list of CookieRules.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_SPYWARE_CR"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.StringRule"
     * column="RULE_ID"
     */
    public List getCookieRules()
    {
        return cookieRules;
    }

    public void setCookieRules(List cookieRules)
    {
        this.cookieRules = cookieRules;
    }

    /**
     * IPMaddr rules.
     *
     * @return the list of Subnet Rules.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_SPYWARE_SR"
     * @hibernate.collection-key
     * column="SETTINGS_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.IPMaddrRule"
     * column="RULE_ID"
     */
    public List getSubnetRules()
    {
        return subnetRules;
    }

    public void setSubnetRules(List subnetRules)
    {
        this.subnetRules = subnetRules;
    }
}
