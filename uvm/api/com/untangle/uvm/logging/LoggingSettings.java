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
//     private int syslogFacility = 16; /* LOCAL_0 */
//     private int syslogThreshold = 6; /* INFORMATIONAL */
    private String syslogProtocol = "UDP";

    // Constructors -----------------------------------------------------------

    public LoggingSettings() { }

    public LoggingSettings(String syslogHost, int syslogPort, String syslogProtocol)
    {
        this.syslogHost = syslogHost;
        this.syslogPort = syslogPort;
//         this.syslogFacility = syslogFacility;
//         this.syslogThreshold = syslogThreshold;
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
     *
     * KERNEL(0, "kernel"),
     * USER(1, "user"),
     * MAIL(2, "mail"),
     * DAEMON(3, "daemon"),
     * SECURITY_0(4, "security 0"),
     * SYSLOG(5, "syslog"),
     * PRINTER(6, "printer"),
     * NEWS(7, "news"),
     * UUCP(8, "uucp"),
     * CLOCK_0(9, "clock 0"),
     * SECURITY_1(10, "security 1"),
     * FTP(11, "ftp"),
     * NTP(12, "ntp"),
     * LOG_AUDIT(13, "log audit"),
     * LOG_ALERT(14, "log alert"),
     * CLOCK_1(15, "clock 1"),
     * LOCAL_0(16, "local 0"),
     * LOCAL_1(17, "local 1"),
     * LOCAL_2(18, "local 2"),
     * LOCAL_3(19, "local 3"),
     * LOCAL_4(20, "local 4"),
     * LOCAL_5(21, "local 5"),
     * LOCAL_6(22, "local 6"),
     * LOCAL_7(23, "local 7");
     */
    //     public int getSyslogFacility() { return syslogFacility; }
    //     public void setSyslogFacility( int syslogFacility ) { this.syslogFacility = syslogFacility; }

    /**
     * Syslog threshold.
     * 
     * EMERGENCY(0, "emergency"),
     * ALERT(1, "alert"),
     * CRITICAL(2, "critical"),
     * ERROR(3, "error"),
     * WARNING(4, "warning"),
     * NOTICE(5, "notice"),
     * INFORMATIONAL(6, "informational"),
     * DEBUG(7, "debug");
     */
    //     public int getSyslogThreshold() { return syslogThreshold; }
    //     public void setSyslogThreshold( int syslogThreshold ) { this.syslogThreshold = syslogThreshold; }

    /**
     * Syslog protocol.
     */
    public String getSyslogProtocol() { return syslogProtocol; }
    public void setSyslogProtocol( String syslogProtocol ) { this.syslogProtocol = syslogProtocol; }
}
