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

    private Long id;
    private String reportEmail;
    private String smtpHost;
    private String fromAddress;

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
	if( smtpHost == null )
	    smtpHost = new String();
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost)
    {
        this.smtpHost = smtpHost;
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
}
