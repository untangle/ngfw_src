/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm;

import java.io.Serializable;

/**
 * MVVM mail settings.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="MAIL_SETTINGS"
 */
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
    private String  smtpHost;
    private int     smtpPort = 25;
    private boolean useTls = false;
    private String  authUser;
    private String  authPass;
    private String  localHostName;

    /**
     * @hibernate.id
     * column="MAIL_SETTINGS_ID"
     * generator-class="native"
     */
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
     *
     * @hibernate.property
     * column="REPORT_EMAIL"
     */
    public String getReportEmail()
    {
    if( reportEmail == null )
        reportEmail = new String();
        return reportEmail;
    }

    public void setReportEmail(String reportEmail)
    {
        this.reportEmail = reportEmail;
    }

    /**
     * The SMTP mail host to use to send internal report and error
     * emails.  This can be a host name or an IP address.  (If not
     * set, all email addresses must be fully qualified, and the
     * normal DNS MX records are used to determine SMTP host).
     *
     * @return a <code>String</code> value
     * @hibernate.property
     * column="SMTP_HOST"
     */
    public String getSmtpHost()
    {
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
     * @return a <code>int</code> giving the TCP port to connect to the SMTP_HOST at
     * @hibernate.property
     * not-null="true"
     * column="SMTP_PORT"
     */
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
     * @hibernate.property
     * column="USE_TLS"
     * not-null="true"
     */
    public boolean isUseTls()
    {
        return useTls;
    }

    public void setUseTls(boolean useTls)
    {
        this.useTls = useTls;
    }

    /**
     * The From address for mail coming from the Metavize system.
     *
     * @return a <code>String</code> value
     * @hibernate.property
     * column="FROM_ADDRESS"
     */
    public String getFromAddress()
    {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress)
    {
        this.fromAddress = fromAddress;
    }

    /**
     * The User to use for SMTP Auth.  If null or if the pass is null, don't use SMTP auth.
     *
     * @return a <code>String</code> giving the user name for SMTP Auth
     * @hibernate.property
     * not-null="true"
     * column="AUTH_USER"
     */
    public String getAuthUser()
    {
        return authUser;
    }

    public void setAuthUser(String authUser)
    {
        this.authUser = authUser;
    }

    /**
     * The Password to use for SMTP Auth.  If null or if the user is null, don't use SMTP auth.
     *
     * @return a <code>String</code> giving the password for SMTP Auth
     * @hibernate.property
     * not-null="true"
     * column="AUTH_PASS"
     */
    public String getAuthPass()
    {
        return authPass;
    }

    public void setAuthPass(String authPass)
    {
        this.authPass = authPass;
    }

    /**
     * The Local host name to use for sending (SMTP HELO).  If null, use the actual
     * local host name (currently always mv-edgeguard).
     *
     * @return a <code>String</code> giving the local host name for HELO
     * @hibernate.property
     * not-null="true"
     * column="LOCAL_HOST_NAME"
     */
    public String getLocalHostName()
    {
        return localHostName;
    }

    public void setLocalHostName(String localHostName)
    {
        this.localHostName = localHostName;
    }
}
