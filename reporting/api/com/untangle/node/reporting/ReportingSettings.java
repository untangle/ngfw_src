/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.reporting;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.untangle.uvm.node.IPMaddrDirectory;
import com.untangle.uvm.security.Tid;

/**
 * Settings for the Reporting Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_reporting_settings", schema="settings")
public class ReportingSettings implements Serializable
{
    private static final long serialVersionUID = 2064742840204258977L;

    private Long id;
    private Tid tid;

    private IPMaddrDirectory networkDirectory = new IPMaddrDirectory();
    private int daysToKeep = 7;
    private boolean emailDetail = false;

    private String reportingUsers;

    public ReportingSettings() { }

    @Id
    @Column(name="id")
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Node id for these settings.
     *
     * @return tid for these settings
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Network Directory (maps IP addresses to reporting names)
     *
     * @return the network directory
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="network_directory", nullable=false)
    public IPMaddrDirectory getNetworkDirectory()
    {
        return networkDirectory;
    }

    public void setNetworkDirectory(IPMaddrDirectory networkDirectory)
    {
        this.networkDirectory = networkDirectory;
    }

    @Column(name="days_to_keep", nullable=false)
    public int getDaysToKeep()
    {
        return daysToKeep;
    }

    public void setDaysToKeep(int daysToKeep)
    {
        this.daysToKeep = daysToKeep;
    }

    @Column(name="email_detail", nullable=false)
    public boolean getEmailDetail()
    {
        return emailDetail;
    }

    public void setEmailDetail(boolean emailDetail)
    {
        this.emailDetail = emailDetail;
    }

    /**
     * Email address of all of the reporting users (these are both
     * users that get emails + users who can access the online
     * reports.
     */
    @Column(name="reporting_users", nullable=true)
    public String getReportingUsers()
    {
        return this.reportingUsers;
    }

    public void setReportingUsers(String newValue)
    {
        this.reportingUsers = newValue;
    }
}
