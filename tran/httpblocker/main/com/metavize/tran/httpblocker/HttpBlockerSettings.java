/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.httpblocker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.metavize.mvvm.security.Tid;

/**
 * HttpBlocker settings.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_HTTPBLK_SETTINGS"
 */
public class HttpBlockerSettings implements Serializable
{
    private static final long serialVersionUID = 1806394002255164868L;

    private Long id;
    private Tid tid;

    private BlockTemplate blockTemplate = new BlockTemplate();

    private boolean blockAllIpHosts = false;

    private boolean fascistMode = false;

    private List passedClients = new ArrayList();
    private List passedUrls = new ArrayList();

    private List blockedUrls = new ArrayList();
    private List blockedMimeTypes = new ArrayList();
    private List blockedExtensions = new ArrayList();
    private List blacklistCategories = new ArrayList();

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public HttpBlockerSettings() { }

    public HttpBlockerSettings(Tid tid)
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
        for (Iterator i = blacklistCategories.iterator(); i.hasNext(); ) {
            BlacklistCategory bc = (BlacklistCategory)i.next();
            if (bc.getName() == name) {
                return bc;
            }
        }

        return null;
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
     * @return tid for these settings.
     * @hibernate.many-to-one
     * column="TID"
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
     * Template for block messages.
     *
     * @return the block message.
     * @hibernate.many-to-one
     * column="TEMPLATE"
     * cascade="all"
     * not-null="true"
     */
    public BlockTemplate getBlockTemplate()
    {
        return blockTemplate;
    }

    public void setBlockTemplate(BlockTemplate blockTemplate)
    {
        this.blockTemplate = blockTemplate;
    }

    /**
     * Block all requests to hosts identified only by an IP address.
     *
     * @return true when IP requests are blocked.
     * @hibernate.property
     * column="BLOCK_ALL_IP_HOSTS"
     * not-null="true"
     */
    public boolean getBlockAllIpHosts()
    {
        return blockAllIpHosts;
    }

    public void setBlockAllIpHosts(boolean blockAllIpHosts)
    {
        this.blockAllIpHosts = blockAllIpHosts;
    }

    /**
     * If true, block everything that is not whitelisted.
     *
     * @return true to block.
     * @hibernate.property
     * column="FASCIST_MODE"
     */
    public boolean getFascistMode()
    {
        return fascistMode;
    }

    public void setFascistMode(boolean fascistMode)
    {
        this.fascistMode = fascistMode;
    }

    /**
     * Clients not subject to blacklisting.
     *
     * @return the list of passed clients.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_HTTPBLK_PASSED_CLIENTS"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.IPMaddrRule"
     * column="RULE_ID"
     */
    public List getPassedClients()
    {
        return passedClients;
    }

    public void setPassedClients(List passedClients)
    {
        this.passedClients = passedClients;
    }

    /**
     * URLs not subject to blacklist checking.
     *
     * @return the list of passed URLs.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_HTTPBLK_PASSED_URLS"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.StringRule"
     * column="RULE_ID"
     */
    public List getPassedUrls()
    {
        return passedUrls;
    }

    public void setPassedUrls(List passedUrls)
    {
        this.passedUrls = passedUrls;
    }

    /**
     * URLs not subject to blacklist checking.
     *
     * @return the list of blocked URLs.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_HTTPBLK_BLOCKED_URLS"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.StringRule"
     * column="RULE_ID"
     */
    public List getBlockedUrls()
    {
        return blockedUrls;
    }

    public void setBlockedUrls(List blockedUrls)
    {
        this.blockedUrls = blockedUrls;
    }

    /**
     * Mime-Types to be blocked.
     *
     * @return the list of blocked MIME types.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_HTTPBLK_MIME_TYPES"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.MimeTypeRule"
     * column="RULE_ID"
     */
    public List getBlockedMimeTypes()
    {
        return blockedMimeTypes;
    }

    public void setBlockedMimeTypes(List blockedMimeTypes)
    {
        this.blockedMimeTypes = blockedMimeTypes;
    }

    /**
     * Extensions to be blocked.
     *
     * @return the list of blocked extensions.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_HTTPBLK_EXTENSIONS"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.StringRule"
     * column="RULE_ID"
     */
    public List getBlockedExtensions()
    {
        return blockedExtensions;
    }

    public void setBlockedExtensions(List blockedExtensions)
    {
        this.blockedExtensions = blockedExtensions;
    }

    /**
     * Blacklist blocking options.
     *
     * @return the list of blacklist categories.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-one-to-many
     * class="com.metavize.tran.httpblocker.BlacklistCategory"
     */
    public List getBlacklistCategories()
    {
        return blacklistCategories;
    }

    public void setBlacklistCategories(List blacklistCategories)
    {
        this.blacklistCategories = blacklistCategories;
    }
}
