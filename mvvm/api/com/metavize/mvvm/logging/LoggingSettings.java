/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;

import java.io.Serializable;

/**
 * Settings for the LoggingManager.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="LOGGING_SETTINGS"
 */
public class LoggingSettings implements Serializable
{
    private Long id;
    private boolean syslogEnabled = false;
    private String syslogHost;
    private int syslogPort = 514;
    private SyslogFacility syslogFacility = SyslogFacility.LOCAL_0;
    private SyslogPriority syslogThreshold = SyslogPriority.INFORMATIONAL;

    // Constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public LoggingSettings() { }

    public LoggingSettings(String syslogHost, int syslogPort,
                           SyslogFacility syslogFacility,
                           SyslogPriority syslogThreshold)
    {
        this.syslogHost = syslogHost;
        this.syslogPort = syslogPort;
        this.syslogFacility = syslogFacility;
        this.syslogThreshold = syslogThreshold;
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
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
     * Packets are sent when syslogEnabled is true.
     *
     * @return true if syslog is enabled, false otherwise.
     * @hibernate.property
     * column="SYSLOG_ENABLED"
     */
    public boolean isSyslogEnabled()
    {
        return syslogEnabled;
    }

    public void setSyslogEnabled(boolean syslogEnabled)
    {
        this.syslogEnabled = syslogEnabled;
    }

    /**
     * Syslog destination hostname.
     *
     * @return the Syslog host.
     * @hibernate.property
     * column="SYSLOG_HOST"
     */
    public String getSyslogHost()
    {
        return syslogHost;
    }

    public void setSyslogHost(String syslogHost)
    {
        this.syslogHost = syslogHost;
    }

    /**
     * Syslog destination port.
     *
     * @return the Syslog port.
     * @hibernate.property
     * column="SYSLOG_PORT"
     */
    public int getSyslogPort()
    {
        return syslogPort;
    }

    public void setSyslogPort(int syslogPort)
    {
        this.syslogPort = syslogPort;
    }


    /**
     * Facility to log as.
     *
     * @return the Syslog facility.
     * @hibernate.property
     * type="com.metavize.mvvm.type.SyslogFacilityUserType"
     * column="SYSLOG_FACILITY"
     */
    public SyslogFacility getSyslogFacility()
    {
        return syslogFacility;
    }

    public void setSyslogFacility(SyslogFacility syslogFacility)
    {
        this.syslogFacility = syslogFacility;
    }

    /**
     * Syslog threshold.
     *
     * @return a <code>SyslogPriority</code> value
     * @hibernate.property
     * type="com.metavize.mvvm.type.SyslogPriorityUserType"
     * column="SYSLOG_THRESHOLD"
     */
    public SyslogPriority getSyslogThreshold()
    {
        return syslogThreshold;
    }

    public void setSyslogThreshold(SyslogPriority syslogThreshold)
    {
        this.syslogThreshold = syslogThreshold;
    }
}
