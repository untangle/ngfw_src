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
import java.util.ArrayList;
import java.util.List;

/**
 * Portal settings, all together.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="Portal_Settings"
 */
public class PortalSettings implements Serializable
{
    private static final long serialVersionUID = -1618117644960894373L;

    private Long id;

    private List users;
    private List groups;

    private PortalGlobal global;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public PortalSettings() { }


    // business methods ------------------------------------------------------
    public PortalUser addUser(String uid)
    {
        PortalUser newUser = new PortalUser(uid, null);
        users.add(newUser);
        return newUser;
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
     * The list of portal users associated with this portal configuration.
     *
     * @return the list of vpn portal users.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.portal.PortalUser"
     */
    public List getUsers()
    {
        if (users == null)
            users = new ArrayList();

        return this.users;
    }

    public void setUsers(List users)
    {
        this.users = users;
    }

    /**
     * The list of portal groups associated with this portal configuration.
     *
     * @return the list of vpn portal groups.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.portal.PortalGroup"
     */
    public List getGroups()
    {
        if (groups == null)
            groups = new ArrayList();

        return this.groups;
    }

    public void setGroups(List groups)
    {
        this.groups = groups;
    }

    /**
     * The global portal settings for this portal configuration.
     *
     * @return the PortaGlobal.
     * @hibernate.one-to-one
     * column="global_settings_id"
     */
    public PortalGlobal getGlobal()
    {
        return global;
    }

    public void setGlobal(PortalGlobal Global)
    {
        this.global = global;
    }

}
