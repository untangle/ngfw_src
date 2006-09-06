/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.reporting;

import java.io.Serializable;

import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.IPMaddrDirectory;

/**
 * Settings for the Reporting Transform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_REPORTING_SETTINGS"
 */
public class ReportingSettings implements Serializable
{
    private static final long serialVersionUID = 2064742840204258977L;

    private Long id;
    private Tid tid;

    private boolean emailDetail = false; // do not email detail info
    private IPMaddrDirectory networkDirectory = new IPMaddrDirectory();
    private Schedule schedule = new Schedule();

    public ReportingSettings() { }

    /**
     * @hibernate.id
     * column="ID"
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
     * Transform id for these settings.
     *
     * @return tid for these settings
     * @hibernate.many-to-one
     * column="TID"
     * not-null="true"
     */
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
     * @hibernate.property
     * column="EMAIL_DETAIL"
     * not-null="true"
     */
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
     * @hibernate.many-to-one
     * column="NETWORK_DIRECTORY"
     * cascade="all"
     * not-null="true"
     */
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
     * @hibernate.many-to-one
     * column="SCHEDULE"
     * cascade="all"
     * not-null="true"
     */
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
