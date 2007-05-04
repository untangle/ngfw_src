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

package com.untangle.mvvm.portal;

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
 * Portal group.  Group name is free, and currently has no
 * relationship to addrress book.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@Entity
@Table(name="portal_group", schema="settings")
public class PortalGroup implements Serializable
{
    private static final long serialVersionUID = -1681114764096389437L;

    private Long id;
    private String name;
    private String description;

    private PortalHomeSettings portalHomeSettings;
    private List<Bookmark> bookmarks = new ArrayList<Bookmark>();

    // constructors -----------------------------------------------------------

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
     * Get a name for display purposes.
     *
     * @return name.
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
     * description/comments
     *
     * @return the recorded comments
     */
    public String getDescription()
    {
        if (description == null) {
            return "";
        } else {
            return description;
        }
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * The PortalHomeSettings that this group has.  This may be null,
     * in which case the global settings are used.  Thus, this should
     * remain null until the user changes some value -- the UI should
     * show the global settings until that point.
     *
     * @return the PortalHomeSettings.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="home_settings_id")
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
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="portal_group_bm_mt",
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

    // Object methods ----------------------------------------------------------

    public int hashCode()
    {
        if (name == null)
            // shouldn't happen
            return 0;

        return name.hashCode();
    }

    public boolean equals(Object obj)
    {
        if( !(obj instanceof PortalGroup) )
            return false;
        PortalGroup other = (PortalGroup) obj;
        return getName().equals(other.getName());
    }
}
