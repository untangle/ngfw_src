/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/webfilter/api/com/untangle/node/webfilter/WebFilterBaseSettings.java $
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
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import com.untangle.node.http.UserWhitelistMode;

/**
 * Base Settings for the WebFilter node.
 *
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
@Embeddable
public class WebFilterBaseSettings implements Serializable {

    private UserWhitelistMode userWhitelistMode = UserWhitelistMode.NONE;

    private BlockTemplate blockTemplate = new BlockTemplate();

    private boolean blockAllIpHosts = false;

    /* Disabling HTTPs by default */
    private boolean enableHttps = false;
    
    private boolean unblockPasswordEnabled = false;
    private boolean unblockPasswordAdmin = false;
    private String unblockPassword = "";

    private boolean enforceSafeSearch = true;

    private int passedClientsLength;
    private int passedUrlsLength;
    private int blockedUrlsLength;
    private int blockedMimeTypesLength;
    private int blockedExtensionsLength;
    private int blacklistCategoriesLength;


    public WebFilterBaseSettings() {}

    /**
     * Template for block messages.
     *
     * @return the block message.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="template", nullable=false)
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
     */
    @Column(name="block_all_ip_hosts", nullable=false)
    public boolean getBlockAllIpHosts()
    {
        return blockAllIpHosts;
    }

    public void setBlockAllIpHosts(boolean blockAllIpHosts)
    {
        this.blockAllIpHosts = blockAllIpHosts;
    }

    /**
     * If true, enables checking of HTTPS traffic.
     *
     * @return true to block.
     */
    @Column(name="enable_https", nullable=false)
    public boolean getEnableHttps()
    {
        return enableHttps;
    }

    public void setEnableHttps(boolean enableHttps)
    {
        this.enableHttps = enableHttps;
    }

    /**
     * If true, enforces safe search on popular search engines.
     *
     * @return true to block.
     */
    @Column(name="enforce_safe_search", nullable=false)
    public boolean getEnforceSafeSearch()
    {
        return enforceSafeSearch;
    }

    public void setEnforceSafeSearch(boolean enforceSafeSearch)
    {
        this.enforceSafeSearch = enforceSafeSearch;
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
     * If true, ask for a password to unblock a site.
     *
     * @return true to block.
     */
    @Column(name="unblock_password_enabled", nullable=false)
    public boolean getUnblockPasswordEnabled()
    {
        return this.unblockPasswordEnabled;
    }

    public void setUnblockPasswordEnabled(boolean newValue)
    {
        this.unblockPasswordEnabled = newValue;
    }

    /**
     * If true, use the administrative password to unblock a site.
     *
     * @return true to use the administrative password.
     */
    @Column(name="unblock_password_admin", nullable=false)
    public boolean getUnblockPasswordAdmin()
    {
        return this.unblockPasswordAdmin;
    }

    public void setUnblockPasswordAdmin(boolean newValue)
    {
        this.unblockPasswordAdmin = newValue;
    }

    /**
     * String to use for the unblock password
     *
     * @return Unblock password for this node..
     */
    @Column(name="unblock_password", nullable=false)
    public String getUnblockPassword()
    {
        return this.unblockPassword;
    }

    public void setUnblockPassword(String newValue)
    {
        this.unblockPassword = newValue;
    }

    @Transient
    public int getPassedClientsLength() {
        return passedClientsLength;
    }

    public void setPassedClientsLength(int passedClientsLength) {
        this.passedClientsLength = passedClientsLength;
    }

    @Transient
    public int getPassedUrlsLength() {
        return passedUrlsLength;
    }

    public void setPassedUrlsLength(int passedUrlsLength) {
        this.passedUrlsLength = passedUrlsLength;
    }

    @Transient
    public int getBlockedUrlsLength() {
        return blockedUrlsLength;
    }

    public void setBlockedUrlsLength(int blockedUrlsLength) {
        this.blockedUrlsLength = blockedUrlsLength;
    }

    @Transient
    public int getBlockedMimeTypesLength() {
        return blockedMimeTypesLength;
    }

    public void setBlockedMimeTypesLength(int blockedMimeTypesLength) {
        this.blockedMimeTypesLength = blockedMimeTypesLength;
    }

    @Transient
    public int getBlockedExtensionsLength() {
        return blockedExtensionsLength;
    }

    public void setBlockedExtensionsLength(int blockedExtensionsLength) {
        this.blockedExtensionsLength = blockedExtensionsLength;
    }

    @Transient
    public int getBlacklistCategoriesLength() {
        return blacklistCategoriesLength;
    }

    public void setBlacklistCategoriesLength(int blacklistCategoriesLength) {
        this.blacklistCategoriesLength = blacklistCategoriesLength;
    }

}
