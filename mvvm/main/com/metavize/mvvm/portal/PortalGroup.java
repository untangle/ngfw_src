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
 * Portal group.  Group name is free, and currently has no relationship to addrress book.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="Portal_Group"
 */
public class PortalGroup implements Serializable
{
    private static final long serialVersionUID = -1681114764096389437L;

    private Long id;
    private String name;
    private String description;

    private PortalHomeSettings portalHomeSettings;
    private List bookmarks;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public PortalGroup() { }

    /**
     * Constructor does not check that the name is valid.
     *
     * @param name a <code>String</code> value
     */
    public PortalGroup(String name)
    {
        this.name = name;
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
     * Get a name for display purposes.
     *
     * @return name.
     * @hibernate.property
     * column="NAME"
     */
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * The PortalHomeSettings that this group has.  This may be null, in which case
     * the global settings are used.
     * Thus, this should remain null until the user changes some value -- the UI should
     * show the global settings until that point.
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
     * @return the list of bookmarks for this group.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="PORTAL_GROUP_BM_MT"
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

    public boolean equals(Object obj)
    {
	if( !(obj instanceof PortalGroup) )
	    return false;
	PortalGroup other = (PortalGroup) obj;
	return getName().equals(other.getName());       
    }
}
