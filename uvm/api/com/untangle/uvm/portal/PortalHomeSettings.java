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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Portal Home Settings -- customization of portal home page.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@Entity
@Table(name="n_portal_home_settings", schema="settings")
public class PortalHomeSettings implements Serializable
{
    private static final long serialVersionUID = -4618114760496839437L;

    private Long id;

    private String homePageTitle = "Remote Access Portal";
    private String homePageText = "Welcome to the Untangle Remote Access Portal." ;
    private String bookmarkTableTitle = "Bookmarks";
    private boolean showExploder = true;
    private boolean showBookmarks = true;
    private boolean showAddBookmark = true;
    // private boolean showImportCert;
    private long idleTimeout = 20 * 60 * 1000; // 20 minutes
    public static final long IDLE_TIMEOUT_MIN = 1 * 60 * 1000; // 1 minute
    public static final long IDLE_TIMEOUT_DEFAULT = 20 * 60 * 1000; // 20 minutes
    public static final long IDLE_TIMEOUT_MAX = 24 * 60 * 60 * 1000; // 1 day

    // constructors -----------------------------------------------------------

    public PortalHomeSettings() { }

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
     * Get a home page title for display purposes.
     *
     * @return name.
     */
    @Column(name="home_page_title")
    public String getHomePageTitle()
    {
        return homePageTitle;
    }

    public void setHomePageTitle(String homePageTitle)
    {
        this.homePageTitle = homePageTitle;
    }

    /**
     * Get a home page text for display purposes.
     *
     * @return name.
     */
    @Column(name="home_page_text")
    public String getHomePageText()
    {
        return homePageText;
    }

    public void setHomePageText(String homePageText)
    {
        this.homePageText = homePageText;
    }

    /**
     * Get the bookmark tabletitle for display purposes.
     *
     * @return name.
     */
    @Column(name="bookmark_table_title")
    public String getBookmarkTableTitle()
    {
        return bookmarkTableTitle;
    }

    public void setBookmarkTableTitle(String bookmarkTableTitle)
    {
        this.bookmarkTableTitle = bookmarkTableTitle;
    }

    /**
     * should the file explorer be launchable (have a button on the home page)
     *
     * @return true if this portal home is allowed to use the exploder
     */
    @Column(name="show_exploder", nullable=false)
    public boolean isShowExploder()
    {
        return showExploder;
    }

    public void setShowExploder(boolean showExploder)
    {
        this.showExploder = showExploder;
    }

    /**
     * should the bookmarks table be shown on the home page)
     *
     * @return true if this portal home is allowed to use bookmarks
     */
    @Column(name="show_bookmarks", nullable=false)
    public boolean isShowBookmarks()
    {
        return showBookmarks;
    }

    public void setShowBookmarks(boolean showBookmarks)
    {
        this.showBookmarks = showBookmarks;
    }

    /**
     * should the add new Bookmarks button be shown on the home page)
     * Note that this only makes sense if showBookmsks is enabled.  Otherwise
     * this value doesn't matter.
     *
     * @return true if this portal home is allowed to add new bookmarks
     */
    @Column(name="show_add_bookmark", nullable=false)
    public boolean isShowAddBookmark()
    {
        return showAddBookmark;
    }

    public void setShowAddBookmark(boolean showAddBookmark)
    {
        this.showAddBookmark = showAddBookmark;
    }

    /**
     * what is the idle timeout for this portal home
     *
     * @return the idle timeout in milliseconds
     */
    @Column(name="idle_timeout", nullable=false)
    public long getIdleTimeout()
    {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout)
    {
        this.idleTimeout = idleTimeout;
    }
}
