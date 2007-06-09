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

package com.untangle.tran.reporting;

import java.io.Serializable;
import java.util.Calendar;
import java.util.LinkedList;
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
import javax.persistence.Transient;

import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.IPMaddrDirectory;

/**
 * Settings for the Reporting Transform.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_reporting_settings", schema="settings")
public class ReportingSettings implements Serializable
{
    private static final long serialVersionUID = 2064742840204258977L;

    private Long id;
    private Tid tid;

    private boolean emailDetail = false; // do not email detail info
    private IPMaddrDirectory networkDirectory = new IPMaddrDirectory();
    private Schedule schedule = new Schedule();

    public ReportingSettings() { }

    @Id
    @Column(name="id")
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
     * Transform id for these settings.
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
     * email detail info with reports
     * - false = do not email detail info, true = do email detail info
     *
     * @return email detail
     */
    @Column(name="email_detail", nullable=false)
    public boolean getEmailDetail()
    {
        return emailDetail;
    }

    public void setEmailDetail(boolean emailDetail)
    {
        this.emailDetail = emailDetail;
        return;
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

    /**
     * Schedule (daily, weekly, monthly) for reports
     *
     * @return schedule for reports
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="schedule", nullable=false)
    public Schedule getSchedule()
    {
        return schedule;
    }

    public void setSchedule(Schedule schedule)
    {
        this.schedule = schedule;
        return;
    }
}
