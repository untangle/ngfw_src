/**
 * $Id$
 */
package com.untangle.node.reporting;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.untangle.uvm.node.IPMaskedAddressDirectory;
import com.untangle.uvm.security.NodeId;

/**
 * Settings for the Reporting Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_reporting_settings", schema="settings")
@SuppressWarnings("serial")
public class ReportingSettings implements Serializable
{
    private Long id;
    private NodeId tid;

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

    @Id
    @Column(name="id")
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
     * Node id for these settings.
     *
     * @return tid for these settings
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public NodeId getNodeId()
    {
        return tid;
    }

    public void setNodeId(NodeId tid)
    {
        this.tid = tid;
    }

    /**
     * Network Directory (maps IP addresses to reporting names)
     *
     * @return the network directory
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="network_directory", nullable=false)
    public IPMaskedAddressDirectory getNetworkDirectory()
    {
        return networkDirectory;
    }

    public void setNetworkDirectory(IPMaskedAddressDirectory networkDirectory)
    {
        this.networkDirectory = networkDirectory;
    }

    @Column(name="db_retention", nullable=false)
    public int getDbRetention()
    {
        return dbRetention;
    }

    public void setDbRetention(int dbRetention)
    {
        this.dbRetention = dbRetention;
    }

    @Column(name="nightly_hour", nullable=false)
    public int getNightlyHour() { return nightlyHour; }
    public void setNightlyHour(int nightlyHour) { this.nightlyHour = nightlyHour; }

    @Column(name="nightly_minute", nullable=false)
    public int getNightlyMinute() { return nightlyMinute; }
    public void setNightlyMinute(int nightlyMinute) { this.nightlyMinute = nightlyMinute; }

    @Column(name="file_retention", nullable=false)
    public int getFileRetention()
    {
        return fileRetention;
    }

    public void setFileRetention(int fileRetention)
    {
        this.fileRetention = fileRetention;
    }

    @Column(name="email_detail", nullable=false)
    public boolean getEmailDetail()
    {
        return emailDetail;
    }

    public void setEmailDetail(boolean emailDetail)
    {
        this.emailDetail = emailDetail;
    }

    @Column(name="attachment_size_limit", nullable=false)
    public int getAttachmentSizeLimit()
    {
        return attachmentSizeLimit;
    }

    public void setAttachmentSizeLimit(int limit)
    {
        attachmentSizeLimit = limit;
    }

    /**
     * Email address of all of the reporting users (these are both
     * users that get emails + users who can access the online
     * reports.
     */
    @Column(name="reporting_users", nullable=true)
    public String getReportingUsers()
    {
        return this.reportingUsers;
    }

    public void setReportingUsers(String newValue)
    {
        this.reportingUsers = newValue;
    }

    /**
     * Schedule (daily, weekly, monthly) for reports
     *
     * @return schedule for reports
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="schedule", nullable=false)
    public Schedule getSchedule()
    {
        return schedule;
    }

    public void setSchedule(Schedule schedule)
    {
        this.schedule = schedule;
    }
}
