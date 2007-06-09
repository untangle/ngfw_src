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

package com.untangle.uvm.portal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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

/**
 * Portal global.  Global settings for portal.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@Entity
@Table(name="portal_global", schema="settings")
public class PortalGlobal implements Serializable
{
    private static final long serialVersionUID = -5681117464960398347L;

    private Long id;
    private boolean autoCreateUsers = true;

    private String loginPageTitle = "Portal Login";
    private String loginPageText = "";

    private PortalHomeSettings portalHomeSettings;
    private List bookmarks = new ArrayList();

    // constructors -----------------------------------------------------------

    public PortalGlobal() { }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="id")
    @GeneratedValue
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
     */
    @Column(name="auto_create_users", nullable=false)
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
     */
    @Column(name="login_page_title")
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
     */
    @Column(name="login_page_text")
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="home_settings_id", nullable=false)
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
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="portal_global_bm_mt",
               joinColumns=@JoinColumn(name="settings_id"),
               inverseJoinColumns=@JoinColumn(name="bookmark_id"))
    @IndexColumn(name="position")
    public List<Bookmark> getBookmarks()
    {
        return bookmarks;
    }

    public void setBookmarks(List<Bookmark> bookmarks)
    {
        this.bookmarks = bookmarks;
    }
}
