/**
 * $Id$
 */
package com.untangle.node.reporting;

import java.io.Serializable;

import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.IPMaskedAddressDirectory;

/**
 * Settings for the Reporting Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class ReportingSettings implements Serializable
{
    private Long id;
    private Long nodeId;

    private IPMaskedAddressDirectory networkDirectory = new IPMaskedAddressDirectory();
    private Integer dbRetention = 7; // days
    private Integer fileRetention = 30; // days
    private Boolean emailDetail = false;

    private Integer nightlyHour = 2;
    private Integer nightlyMinute = 0;
    private Integer attachmentSizeLimit = 10; // MB

    private String reportingUsers;

    private Schedule schedule = new Schedule();

    public ReportingSettings() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    /**
     * Node id for these settings.
     *
     * @return tid for these settings
     */
    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }

    /**
     * Network Directory (maps IP addresses to reporting names)
     *
     * @return the network directory
     */
    public IPMaskedAddressDirectory getNetworkDirectory() { return networkDirectory; }
    public void setNetworkDirectory(IPMaskedAddressDirectory networkDirectory) { this.networkDirectory = networkDirectory; }

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
    public String getReportingUsers() { return this.reportingUsers; }
    public void setReportingUsers(String newValue) { this.reportingUsers = newValue; }

    /**
     * Schedule (daily, weekly, monthly) for reports
     *
     * @return schedule for reports
     */
    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }
}
