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

package com.untangle.uvm.logging;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * Settings for the LoggingManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="logging_settings", schema="settings")
public class LoggingSettings implements Serializable
{
    private Long id;
    private boolean syslogEnabled = false;
    private String syslogHost;
    private int syslogPort = 514;
    private SyslogFacility syslogFacility = SyslogFacility.LOCAL_0;
    private SyslogPriority syslogThreshold = SyslogPriority.INFORMATIONAL;

    // Constructors -----------------------------------------------------------

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

    @Id
    @Column(name="settings_id")
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
     * Packets are sent when syslogEnabled is true.
     *
     * @return true if syslog is enabled, false otherwise.
     */
    @Column(name="syslog_enabled", nullable=false)
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
     */
    @Column(name="syslog_host")
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
     */
    @Column(name="syslog_port", nullable=false)
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
     */
    @Column(name="syslog_facility")
    @Type(type="com.untangle.uvm.type.SyslogFacilityUserType")
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
     */
    @Column(name="syslog_threshold")
    @Type(type="com.untangle.uvm.type.SyslogPriorityUserType")
    public SyslogPriority getSyslogThreshold()
    {
        return syslogThreshold;
    }

    public void setSyslogThreshold(SyslogPriority syslogThreshold)
    {
        this.syslogThreshold = syslogThreshold;
    }
}
