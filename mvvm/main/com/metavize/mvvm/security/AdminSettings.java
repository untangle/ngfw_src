/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: AdminSettings.java,v 1.6 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.mvvm.security;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.metavize.mvvm.Period;

/**
 * Mvvm administrator settings.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="ADMIN_SETTINGS"
 */
public class AdminSettings implements Serializable
{
    private static final long serialVersionUID = -6013161516125662391L;

    private Long id;
    private Set users = new HashSet();
    private Period summaryPeriod;

    /**
     * @hibernate.id
     * column="ADMIN_SETTINGS_ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Specifies a set of system administrators with login access to
     * the system.
     *
     * @return system users.
     * @hibernate.set
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="ADMIN_SETTING_ID"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.security.User"
     */
    public Set getUsers()
    {
        return users;
    }

    public void setUsers(Set users)
    {
        this.users = users;
    }

    public void getUsers(Set users)
    {
        this.users = users;
    }

    public void addUser(User user)
    {
        users.add(user);
    }

    /**
     * Specifies how often summary alerts/reports are generated.
     *
     * @return the summary period.
     * @hibernate.many-to-one
     * column="SUMMARY_PERIOD_ID"
     * cascade="all"
     */
    public Period getSummaryPeriod()
    {
        return summaryPeriod;
    }

    public void setSummaryPeriod(Period summaryPeriod)
    {
        this.summaryPeriod = summaryPeriod;
    }
}
