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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * Portal settings, all together.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@Entity
@Table(name="portal_settings", schema="settings")
public class PortalSettings implements Serializable
{
    private static final long serialVersionUID = -1618117644960894373L;

    private Long id;

    private List<PortalUser> users;
    private List<PortalGroup> groups;

    private PortalGlobal global;

    // constructors -----------------------------------------------------------

    public PortalSettings() { }

    public static PortalSettings getBlankSettings()
    {
        PortalSettings ps = new PortalSettings();
        PortalGlobal pg = new PortalGlobal();
        pg.setPortalHomeSettings(new PortalHomeSettings());
        ps.setGlobal(pg);
        return ps;
    }

    // business methods ------------------------------------------------------

    public PortalUser addUser(String uid)
    {
        PortalUser newUser = new PortalUser(uid, null);
        if (users == null) {
            users = new ArrayList();
        }
        users.add(newUser);
        return newUser;
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
     * The list of portal users associated with this portal configuration.
     *
     * @return the list of vpn portal users.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
            org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<PortalUser> getUsers()
    {
        if (users == null) {
            users = new ArrayList<PortalUser>();
        }

        return this.users;
    }

    public void setUsers(List<PortalUser> users)
    {
        this.users = users;
    }

    /**
     * The list of portal groups associated with this portal configuration.
     *
     * @return the list of vpn portal groups.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
            org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<PortalGroup> getGroups()
    {
        if (groups == null)
            groups = new ArrayList<PortalGroup>();

        return this.groups;
    }

    public void setGroups(List<PortalGroup> groups)
    {
        this.groups = groups;
    }

    /**
     * The global portal settings for this portal configuration.
     *
     * @return the PortalGlobal.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="global_settings_id", nullable=false)
    public PortalGlobal getGlobal()
    {
        return global;
    }

    public void setGlobal(PortalGlobal global)
    {
        this.global = global;
    }
}
