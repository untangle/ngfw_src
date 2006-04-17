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

/**
 * Portal Home Settings -- customization of portal home page.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="Portal_Home_Settings"
 */
public class PortalHomeSettings implements Serializable
{
    private static final long serialVersionUID = -4618114760496839437L;

    private Long id;

    private String homePageTitle = "Portal Home";
    private String homePageText = "";
    private String bookmarkTableTitle = "Bookmarks";
    private boolean showExploder = true;
    private boolean showBookmarks = true;
    private boolean showAddBookmark = true;
    // private boolean showImportCert;
    private long idleTimeout = 20 * 60 * 1000; // 20 minutes

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public PortalHomeSettings() { }



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
     * Get a home page title for display purposes.
     *
     * @return name.
     * @hibernate.property
     * column="HOME_PAGE_TITLE"
     */
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
     * @hibernate.property
     * column="HOME_PAGE_TEXT"
     */
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
     * @hibernate.property
     * column="BOOKMARK_TABLE_TITLE"
     */
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
     * @hibernate.property
     * column="SHOW_EXPLODER"
     */
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
     * @hibernate.property
     * column="SHOW_BOOKMARKS"
     */
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
     * @hibernate.property
     * column="SHOW_ADD_BOOKMARK"
     */
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
     * @hibernate.property
     * column="IDLE_TIMEOUT"
     */
    public long getIdleTimeout()
    {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout)
    {
        this.idleTimeout = idleTimeout;
    }
}
