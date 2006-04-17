/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.portal;

import java.io.Serializable;
import java.util.List;

/**
 * Portal global.  Global settings for portal.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="Portal_Global"
 */
public class PortalGlobal implements Serializable
{
    private static final long serialVersionUID = -5681117464960398347L;

    private Long id;
    private boolean autoCreateUsers = true;

    private String loginPageTitle = "Portal Login";
    private String loginPageText = "";

    private PortalHomeSettings portalHomeSettings;
    private List bookmarks;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public PortalGlobal() { }



    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    protected Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }

    /**
     * should users in address book automatically be allowed access to portal?
     *
     * @return true if this user is allowed to use the portal
     * @hibernate.property
     * column="AUTO_CREATE_USERS"
     */
    public boolean isAutoCreateUsers()
    {
        return autoCreateUsers;
    }

    public void setAutoCreateUsers(boolean autoCreateUsers)
    {
        this.autoCreateUsers = autoCreateUsers;
    }

    /**
     * Get a loginPageTitle for display purposes.
     *
     * @return loginPageTitle.
     * @hibernate.property
     * column="LOGIN_PAGE_TITLE"
     */
    public String getLoginPageTitle()
    {
        return loginPageTitle;
    }

    public void setLoginPageTitle(String loginPageTitle)
    {
        this.loginPageTitle = loginPageTitle;
    }

    /**
     * Get a loginPageText for display purposes.
     *
     * @return loginPageText.
     * @hibernate.property
     * column="LOGIN_PAGE_TEXT"
     */
    public String getLoginPageText()
    {
        return loginPageText;
    }

    public void setLoginPageText(String loginPageText)
    {
        this.loginPageText = loginPageText;
    }

    /**
     * The global PortalHomeSettings (used for all that don't have a group portal home
     * settings customization).
     *
     * @return the PortalHomeSettings.
     * @hibernate.one-to-one
     * column="home_settings_id"
     */
    public PortalHomeSettings getPortalHomeSettings()
    {
        return portalHomeSettings;
    }

    public void setPortalHomeSettings(PortalHomeSettings portalHomeSettings)
    {
        this.portalHomeSettings = portalHomeSettings;
    }

    /**
     * List of bookmarks
     *
     * @return the list of bookmarks for this global.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="PORTAL_GLOBAL_BM_MT"
     * @hibernate.collection-key
     * column="SETTINGS_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.portal.Bookmark"
     * column="BOOKMARK_ID"
     */
    public List getBookmarks()
    {
        return bookmarks;
    }

    public void setBookmarks(List bookmarks)
    {
        this.bookmarks = bookmarks;
    }
}
