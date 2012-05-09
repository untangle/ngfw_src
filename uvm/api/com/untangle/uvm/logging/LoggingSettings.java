/**
 * $Id$
 */
package com.untangle.uvm.logging;

import java.io.Serializable;

/**
 * Settings for the LoggingManager.
 */
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

    public Long getId() { return id; }
    public void setId( Long id ) { this.id = id; }

    /**
     * Packets are sent when syslogEnabled is true.
     */
    public boolean isSyslogEnabled() { return syslogEnabled; }
    public void setSyslogEnabled( boolean syslogEnabled ) { this.syslogEnabled = syslogEnabled; }

    /**
     * Syslog destination hostname.
     */
    public String getSyslogHost() { return syslogHost; }
    public void setSyslogHost( String syslogHost ) { this.syslogHost = syslogHost; }

    /**
     * Syslog destination port.
     */
    public int getSyslogPort() { return syslogPort; }
    public void setSyslogPort( int syslogPort ) { this.syslogPort = syslogPort; }

    /**
     * Facility to log as.
     */
    public SyslogFacility getSyslogFacility() { return syslogFacility; }
    public void setSyslogFacility( SyslogFacility syslogFacility ) { this.syslogFacility = syslogFacility; }

    /**
     * Syslog threshold.
     */
    public SyslogPriority getSyslogThreshold() { return syslogThreshold; }
    public void setSyslogThreshold( SyslogPriority syslogThreshold ) { this.syslogThreshold = syslogThreshold; }

    /**
     * Syslog protocol.
     */
    public String getSyslogProtocol() { return syslogProtocol; }
    public void setSyslogProtocol( String syslogProtocol ) { this.syslogProtocol = syslogProtocol; }
}
