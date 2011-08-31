/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
@Table(name="u_logging_settings", schema="settings")
@SuppressWarnings("serial")
public class LoggingSettings implements Serializable
{
    private Long id;
    private boolean syslogEnabled = false;
    private String syslogHost;
    private int syslogPort = 514;
    private SyslogFacility syslogFacility = SyslogFacility.LOCAL_0;
    private SyslogPriority syslogThreshold = SyslogPriority.INFORMATIONAL;
    private String syslogProtocol = "UDP";

    // Constructors -----------------------------------------------------------

    public LoggingSettings() { }

    public LoggingSettings(String syslogHost, int syslogPort,
                           SyslogFacility syslogFacility,
                           SyslogPriority syslogThreshold,
                           String syslogProtocol)
    {
        this.syslogHost = syslogHost;
        this.syslogPort = syslogPort;
        this.syslogFacility = syslogFacility;
        this.syslogThreshold = syslogThreshold;
        this.syslogProtocol = syslogProtocol;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="settings_id")
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
    @Type(type="com.untangle.uvm.logging.SyslogPriorityUserType")
    public SyslogPriority getSyslogThreshold()
    {
        return syslogThreshold;
    }

    public void setSyslogThreshold(SyslogPriority syslogThreshold)
    {
        this.syslogThreshold = syslogThreshold;
    }

    /**
     * Syslog protocol.
     *
     * @return the Syslog protocol.
     */
    @Column(name="syslog_protocol")
    public String getSyslogProtocol()
    {
        return syslogProtocol;
    }

    public void setSyslogProtocol(String syslogProtocol)
    {
        this.syslogProtocol = syslogProtocol;
    }
}
