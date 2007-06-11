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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
 * Portal user.  UID must match an addressbook UID (although one could
 * be left around from before, we don't cascade delete portal users
 * when the address book user is deleted (since we couldn't do that
 * for an AD user at all))
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@Entity
@Table(name="n_portal_user", schema="settings")
public class PortalUser implements Serializable
{
    private static final long serialVersionUID = -3861141760496839437L;

    private Long id;
    private String uid;
    private boolean live = true;
    private String description;
    private PortalGroup portalGroup;
    private PortalHomeSettings portalHomeSettings;

    private List<Bookmark> bookmarks = new ArrayList<Bookmark>();

    // constructors -----------------------------------------------------------

    public PortalUser() { }

    /**
     * Constructor does not check that the uid or portal group is valid.
     *
     * @param portalGroup a <code>PortalGroup</code> value
     */
    public PortalUser(String uid, PortalGroup portalGroup)
    {
        this.uid = uid;
        this.portalGroup = portalGroup;
    }

    // business methods ------------------------------------------------------

    public Bookmark addBookmark(String name, Application application,
                                String target)
    {
        Bookmark bm = new Bookmark(name, application, target);
        bookmarks.add(bm);
        return bm;
    }

    public Bookmark editBookmark(Long id, String name, Application application,
                                 String target)
    {
        Bookmark bookmark = null;

        for (Bookmark bm : bookmarks) {
            if (id.equals(bm.getId())) {
                bookmark = bm;
                bookmark.setName(name);
                bookmark.setApplicationName(application.getName());
                bookmark.setTarget(target);
                break;
            }
        }

        return bookmark;
    }

    public void removeBookmark(Bookmark bookmark)
    {
        bookmarks.remove(bookmark);
    }

    public void removeBookmarks(Set<Long> bookmarkIds)
    {
        for (Iterator<Bookmark> i = bookmarks.iterator(); i.hasNext(); ) {
            Bookmark bm = i.next();
            Long id = bm.getId();
            if (null != id && bookmarkIds.contains(id)) {
                i.remove();
            }
        }
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
     */
    @Column(nullable=false)
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
     * The PortalGroup that this user belongs to.  May be null.
     *
     * @return the PortalGroup.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="group_id")
    public PortalGroup getPortalGroup()
    {
        return portalGroup;
    }

    public void setPortalGroup(PortalGroup portalGroup)
    {
        this.portalGroup = portalGroup;
    }

    /**
     * The PortalHomeSettings that this user has.  This may be null,
     * in which case the group or global settings are used.  Thus,
     * this should remain null until the user changes some value --
     * the UI should show the group or global settings until that
     * point.
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
     * @return the list of bookmarks for this user.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="n_portal_user_bm_mt",
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
