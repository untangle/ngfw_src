/**
 * $Id$
 */
package com.untangle.app.reports;

import java.util.LinkedList;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.event.AlertRule;

/**
 * Settings for the Reports App.
 */
@SuppressWarnings("serial")
public class ReportsSettings implements Serializable, JSONString
{
    private Integer version = 2;
    
    private Integer dbRetention = 7; // days
    private Integer generationHour = 2;
    private Integer generationMinute = 0;
    private LinkedList<ReportsHostnameMapEntry> hostnameMap = new LinkedList<ReportsHostnameMapEntry>();
    private LinkedList<ReportsUser> reportsUsers = new LinkedList<ReportsUser>();
    private LinkedList<AlertRule> alertRules = null;
    private LinkedList<EmailTemplate> emailTemplates = new LinkedList<EmailTemplate>();
    
    private String dbHost = "localhost";
    private int    dbPort = 5432;
    private String dbUser = "postgres";
    private String dbPassword = "foo";
    private String dbName = "uvm";
        
    private boolean syslogEnabled = false;
    private String syslogHost;
    private int syslogPort = 514;
    private String syslogProtocol = "UDP";

    private LinkedList<ReportEntry> reportEntries  = new LinkedList<ReportEntry>();

    private boolean googleDriveUploadData = false;
    private boolean googleDriveUploadCsv = false;
    private String  googleDriveDirectory = "Reports Backups";
    
    public ReportsSettings() { }

    public LinkedList<ReportsHostnameMapEntry> getHostnameMap() { return hostnameMap; }
    public void setHostnameMap( LinkedList<ReportsHostnameMapEntry> hostnameMap ) { this.hostnameMap = hostnameMap; }

    public Integer getVersion() { return version; }
    public void setVersion( Integer newValue ) { this.version = newValue; }

    public int getDbRetention() { return dbRetention; }
    public void setDbRetention( int dbRetention ) { this.dbRetention = dbRetention; }

    public int getGenerationHour() { return generationHour; }
    public void setGenerationHour( int generationHour ) { this.generationHour = generationHour; }

    public int getGenerationMinute() { return generationMinute; }
    public void setGenerationMinute( int generationMinute ) { this.generationMinute = generationMinute; }

    public LinkedList<ReportsUser> getReportsUsers() { return this.reportsUsers; }
    public void setReportsUsers( LinkedList<ReportsUser> reportsUsers ) { this.reportsUsers = reportsUsers; }

    public LinkedList<AlertRule> getAlertRules() { return this.alertRules; }
    public void setAlertRules( LinkedList<AlertRule> newValue ) { this.alertRules = newValue; }
    
    public LinkedList<EmailTemplate> getEmailTemplates() { return this.emailTemplates; }
    public void setEmailTemplates( LinkedList<EmailTemplate> newValue ) { this.emailTemplates = newValue; }

    public boolean getSyslogEnabled() { return syslogEnabled; }
    public void setSyslogEnabled( boolean syslogEnabled ) { this.syslogEnabled = syslogEnabled; }

    public LinkedList<ReportEntry> getReportEntries() { return reportEntries; }
    public void setReportEntries( LinkedList<ReportEntry> newValue ) { this.reportEntries = newValue; }

    public String getSyslogHost() { return syslogHost; }
    public void setSyslogHost( String syslogHost ) { this.syslogHost = syslogHost; }

    public int getSyslogPort() { return syslogPort; }
    public void setSyslogPort( int syslogPort ) { this.syslogPort = syslogPort; }

    public String getSyslogProtocol() { return syslogProtocol; }
    public void setSyslogProtocol( String syslogProtocol ) { this.syslogProtocol = syslogProtocol; }

    public String getDbHost() { return dbHost; }
    public void setDbHost( String dbHost ) { this.dbHost = dbHost; }

    public int getDbPort() { return dbPort; }
    public void setDbPort( int dbPort ) { this.dbPort = dbPort; }
    
    public String getDbUser() { return dbUser; }
    public void setDbUser( String dbUser ) { this.dbUser = dbUser; }

    public String getDbPassword() { return dbPassword; }
    public void setDbPassword( String dbPassword ) { this.dbPassword = dbPassword; }

    public String getDbName() { return dbName; }
    public void setDbName( String dbName ) { this.dbName = dbName; }
    
    public boolean getGoogleDriveUploadData() { return googleDriveUploadData; }
    public void setGoogleDriveUploadData( boolean newValue ) { this.googleDriveUploadData = newValue; }

    public boolean getGoogleDriveUploadCsv() { return googleDriveUploadCsv; }
    public void setGoogleDriveUploadCsv( boolean newValue ) { this.googleDriveUploadCsv = newValue; }
    
    public String getGoogleDriveDirectory() { return googleDriveDirectory; }
    public void setGoogleDriveDirectory( String newValue ) { this.googleDriveDirectory = newValue; }

    /**
     * @deprecated
     */
    @Deprecated
    public String getDbDriver() { return null; }
    public void setDbDriver( String dbDriver ) { return; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
