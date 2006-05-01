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
 * Portal user.  UID must match an addressbook UID (although one could be left around
 * from before, we don't cascade delete portal users when the address book user is
 * deleted (since we couldn't do that for an AD user at all))
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="Portal_User"
 */
public class PortalUser implements Serializable
{
    private static final long serialVersionUID = -3861141760496839437L;

    private Long id;
    private String uid;
    private boolean live = true;
    private String description;
    private PortalGroup portalGroup;
    private PortalHomeSettings portalHomeSettings;

    private List bookmarks;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public PortalUser() { }

    /**
     * Constructor does not check that the uid or portal group is valid.
     *
     * @param uid a <code>String</code> value
     * @param portalGroup a <code>PortalGroup</code> value
     */
    public PortalUser(String uid, PortalGroup portalGroup)
    {
        this.uid = uid;
        this.portalGroup = portalGroup;
    }



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
     * Get a uid for display purposes.
     *
     * @return uid.
     * @hibernate.property
     * column="UID"
     */
    public String getUid()
    {
        return uid;
    }

    public void setUid(String uid)
    {
        this.uid = uid;
    }

    /**
     * is the user allowed access?
     *
     * @return true if this user is allowed to use the portal
     * @hibernate.property
     * column="LIVE"
     */
    public boolean isLive()
    {
        return live;
    }

    public void setLive(boolean live)
    {
        this.live = live;
    }

    /**
     * description/comments
     *
     * @return the recorded comments
     * @hibernate.property
     * column="DESCRIPTION"
     */
    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * The PortalGroup that this user belongs to.  May be null.
     *
     * @return the PortalGroup.
     * @hibernate.one-to-one
     * column="group_id"
     */
    public PortalGroup getPortalGroup()
    {
        return portalGroup;
    }

    public void setPortalGroup(PortalGroup portalGroup)
    {
        this.portalGroup = portalGroup;
    }

    /**
     * The PortalHomeSettings that this user has.  This may be null, in which case
     * the group or global settings are used.
     * Thus, this should remain null until the user changes some value -- the UI should
     * show the group or global settings until that point.
     *
     * @return the PortalHomeSettings.
     * @hibernate.one-to-one
     * cascade="all"
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
     * @return the list of bookmarks for this user.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="PORTAL_USER_BM_MT"
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
