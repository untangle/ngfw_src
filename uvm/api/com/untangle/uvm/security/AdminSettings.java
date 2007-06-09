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

package com.untangle.uvm.security;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
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

import com.untangle.uvm.Period;
import org.hibernate.annotations.Cascade;

/**
 * Uvm administrator settings.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="admin_settings", schema="settings")
public class AdminSettings implements Serializable
{
    private static final long serialVersionUID = -6013161516125662391L;

    private Long id;
    private Set<User> users = new HashSet();
    private Period summaryPeriod;

    public AdminSettings() { }

    @Id
    @Column(name="admin_settings_id")
    @GeneratedValue
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
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="admin_setting_id")
    public Set<User> getUsers()
    {
        return users;
    }

    public void setUsers(Set<User> users)
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
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="summary_period_id")
    public Period getSummaryPeriod()
    {
        return summaryPeriod;
    }

    public void setSummaryPeriod(Period summaryPeriod)
    {
        this.summaryPeriod = summaryPeriod;
    }
}
