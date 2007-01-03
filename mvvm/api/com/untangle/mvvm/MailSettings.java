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

package com.untangle.mvvm;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * MVVM mail settings.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="mail_settings", schema="settings")
public class MailSettings implements Serializable
{
    private static final long serialVersionUID = 6722526215093951941L;

    private Long    id;

    // Specific settings for reports
    private String  reportEmail;
    private String  fromAddress;

    // Specific settings for notifications
    // private String  notificationFromAddress;

    // Specific settings for alerts
    // private String  alertEmail;
    // private String  alertFromAddress;

    // Common settings follow
    private boolean useMxRecords = true;
    private String  smtpHost;
    private int     smtpPort = 25;
    private boolean useTls = false;
    private String  authUser;
    private String  authPass;
    private String  localHostName;

    @Id
    @Column(name="mail_settings_id")
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
     * The comma-separated email address(es) to send reports to.
     */
    @Column(name="report_email")
    public String getReportEmail()
    {
        if (reportEmail == null) {
            reportEmail = new String();
        }
        return reportEmail;
    }

    public void setReportEmail(String reportEmail)
    {
        this.reportEmail = reportEmail;
    }

    /**
     * Specifies if we should use MX records or the outgoing mail
     * server to send emails.
     *
     * @return true if should use MX records
     */
    @Column(name="use_mx_records", nullable=false)
    public boolean isUseMxRecords()
    {
        return useMxRecords;
    }

    public void setUseMxRecords(boolean useMxRecords)
    {
        this.useMxRecords = useMxRecords;
    }

    /**
     * The SMTP mail host to use to send internal report and error
     * emails.  This can be a host name or an IP address.
     *
     * @return a <code>String</code> value
     */
    @Column(name="smtp_host")
    public String getSmtpHost()
    {
        if( smtpHost == null )
            return "";
        else
            return smtpHost;
    }

    public void setSmtpHost(String smtpHost)
    {
        this.smtpHost = smtpHost;
    }

    /**
     * The port used to connect to the SMTP mail host.  If the SMTP_HOST
     * is null, this is ignored.
     *
     * @return the TCP port to connect to.
     */
    @Column(name="smtp_port", nullable=false)
    public int getSmtpPort()
    {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort)
    {
        this.smtpPort = smtpPort;
    }

    /**
     * Specifies if we should use TLS if the mail server supports it.
     *
     * @return true if should use TLS when availaable
     */
    @Column(name="use_tls", nullable=false)
    public boolean isUseTls()
    {
        return useTls;
    }

    public void setUseTls(boolean useTls)
    {
        this.useTls = useTls;
    }

    /**
     * The From address for mail coming from the Untangle Platform.
     *
     * @return a <code>String</code> value
     */
    @Column(name="from_address")
    public String getFromAddress()
    {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress)
    {
        this.fromAddress = fromAddress;
    }

    /**
     * The User to use for SMTP Auth.  If null or if the pass is null,
     * don't use SMTP auth.
     *
     * @return a <code>String</code> giving the user name for SMTP Auth
     */
    @Column(name="auth_user")
    public String getAuthUser()
    {
        return authUser;
    }

    public void setAuthUser(String authUser)
    {
        this.authUser = authUser;
    }

    /**
     * The Password to use for SMTP Auth.  If null or if the user is
     * null, don't use SMTP auth.
     *
     * @return a <code>String</code> giving the password for SMTP Auth
     */
    @Column(name="auth_pass")
    public String getAuthPass()
    {
        return authPass;
    }

    public void setAuthPass(String authPass)
    {
        this.authPass = authPass;
    }

    /**
     * The Local host name to use for sending (SMTP HELO).  If null,
     * use the actual local host name (currently always mv-edgeguard).
     *
     * @return a <code>String</code> giving the local host name for HELO
     */
    @Column(name="local_host_name")
    public String getLocalHostName()
    {
        return localHostName;
    }

    public void setLocalHostName(String localHostName)
    {
        this.localHostName = localHostName;
    }
}
