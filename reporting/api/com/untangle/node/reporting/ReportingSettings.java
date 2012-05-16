/**
 * $Id$
 */
package com.untangle.node.reporting;

import java.util.LinkedList;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.DayOfWeekMatcher;

/**
 * Settings for the Reporting Node.
 */
@SuppressWarnings("serial")
public class ReportingSettings implements Serializable, JSONString
{
    private Integer dbRetention = 7; // days
    private Integer fileRetention = 30; // days
    private Integer generationHour = 2;
    private Integer generationMinute = 0;
    private boolean emailDetail = false;
    private Integer attachmentSizeLimit = 10; // MB

    private LinkedList<ReportingHostnameMapEntry> hostnameMap = new LinkedList<ReportingHostnameMapEntry>();
    private LinkedList<ReportingUser> reportingUsers = new LinkedList<ReportingUser>();

    private DayOfWeekMatcher generateDailyReports = new DayOfWeekMatcher("any");;
    private DayOfWeekMatcher generateWeeklyReports = new DayOfWeekMatcher("sunday");;
    private DayOfWeekMatcher generateMonthlyReports = new DayOfWeekMatcher("");;

    private boolean syslogEnabled = false;
    private String syslogHost;
    private int syslogPort = 514;
    private String syslogProtocol = "UDP";
    
    public ReportingSettings() { }

    public LinkedList<ReportingHostnameMapEntry> getHostnameMap() { return hostnameMap; }
    public void setHostnameMap( LinkedList<ReportingHostnameMapEntry> hostnameMap ) { this.hostnameMap = hostnameMap; }

    public int getDbRetention() { return dbRetention; }
    public void setDbRetention( int dbRetention ) { this.dbRetention = dbRetention; }

    public int getGenerationHour() { return generationHour; }
    public void setGenerationHour( int generationHour ) { this.generationHour = generationHour; }

    public int getGenerationMinute() { return generationMinute; }
    public void setGenerationMinute( int generationMinute ) { this.generationMinute = generationMinute; }

    public int getFileRetention() { return fileRetention; }
    public void setFileRetention( int fileRetention ) { this.fileRetention = fileRetention; }

    public boolean getEmailDetail() { return emailDetail; }
    public void setEmailDetail( boolean emailDetail ) { this.emailDetail = emailDetail; }

    public int getAttachmentSizeLimit() { return attachmentSizeLimit; }
    public void setAttachmentSizeLimit( int limit ) { attachmentSizeLimit = limit; }

    public LinkedList<ReportingUser> getReportingUsers() { return this.reportingUsers; }
    public void setReportingUsers( LinkedList<ReportingUser> reportingUsers ) { this.reportingUsers = reportingUsers; }

    public DayOfWeekMatcher getGenerateDailyReports() { return this.generateDailyReports; }
    public void setGenerateDailyReports( DayOfWeekMatcher generateDailyReports ) { this.generateDailyReports = generateDailyReports; }

    public DayOfWeekMatcher getGenerateWeeklyReports() { return this.generateWeeklyReports; }
    public void setGenerateWeeklyReports( DayOfWeekMatcher generateWeeklyReports ) { this.generateWeeklyReports = generateWeeklyReports; }

    public DayOfWeekMatcher getGenerateMonthlyReports() { return this.generateMonthlyReports; }
    public void setGenerateMonthlyReports( DayOfWeekMatcher generateMonthlyReports ) { this.generateMonthlyReports = generateMonthlyReports; }

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

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
