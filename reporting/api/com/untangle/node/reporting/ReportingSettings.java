/**
 * $Id$
 */
package com.untangle.node.reporting;

import java.util.HashMap;
import java.util.LinkedList;
import java.io.Serializable;

import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.IPMaskedAddress;

/**
 * Settings for the Reporting Node.
 */
@SuppressWarnings("serial")
public class ReportingSettings implements Serializable
{
    private HashMap<IPMaskedAddress,String> hostnameMap = new HashMap<IPMaskedAddress,String>();
    private Integer dbRetention = 7; // days
    private Integer fileRetention = 30; // days
    private Boolean emailDetail = false;
    private Integer nightlyHour = 2;
    private Integer nightlyMinute = 0;
    private Integer attachmentSizeLimit = 10; // MB

    private LinkedList<ReportingUser> reportingUsers;

    private Schedule schedule = new Schedule();

    private boolean syslogEnabled = false;
    private String syslogHost;
    private int syslogPort = 514;
    private String syslogProtocol = "UDP";
    
    public ReportingSettings() { }

    /**
     * Network Directory (maps IP addresses to reporting names)
     */
    public HashMap<IPMaskedAddress,String> getHostnameMap() { return hostnameMap; }
    public void setHostnameMap(HashMap<IPMaskedAddress,String> hostnameMap) { this.hostnameMap = hostnameMap; }

    public int getDbRetention() { return dbRetention; }
    public void setDbRetention(int dbRetention) { this.dbRetention = dbRetention; }

    public int getNightlyHour() { return nightlyHour; }
    public void setNightlyHour(int nightlyHour) { this.nightlyHour = nightlyHour; }

    public int getNightlyMinute() { return nightlyMinute; }
    public void setNightlyMinute(int nightlyMinute) { this.nightlyMinute = nightlyMinute; }

    public int getFileRetention() { return fileRetention; }
    public void setFileRetention(int fileRetention) { this.fileRetention = fileRetention; }

    public boolean getEmailDetail() { return emailDetail; }
    public void setEmailDetail(boolean emailDetail) { this.emailDetail = emailDetail; }

    public int getAttachmentSizeLimit() { return attachmentSizeLimit; }
    public void setAttachmentSizeLimit(int limit) { attachmentSizeLimit = limit; }

    /**
     * Email address of all of the reporting users (these are both
     * users that get emails + users who can access the online
     * reports.
     */
    public LinkedList<ReportingUser> getReportingUsers() { return this.reportingUsers; }
    public void setReportingUsers(LinkedList<ReportingUser> reportingUsers) { this.reportingUsers = reportingUsers; }

    /**
     * Schedule (daily, weekly, monthly) for reports
     */
    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }

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
     * Syslog protocol.
     */
    public String getSyslogProtocol() { return syslogProtocol; }
    public void setSyslogProtocol( String syslogProtocol ) { this.syslogProtocol = syslogProtocol; }

}
