/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

/**
 * UVM mail settings.
 */
@SuppressWarnings("serial")
public class MailSettings implements Serializable
{

    private Long id;

    // Specific settings for reports
    private String reportEmail;
    private String fromAddress;

    // Common settings follow
    private boolean useMxRecords = true;
    private String smtpHost;
    private int smtpPort = 25;
    private boolean useTls = false;
    private String authUser;
    private String authPass;
    private String localHostName;

    public Long getId() { return id; }
    public void setId( Long id ) { this.id = id; }

    /**
     * The report recipient email addresses.
     */
    public String getReportEmail()
    {
        if (null == reportEmail) {
            reportEmail = "";
        }

        return reportEmail;
    }

    public void setReportEmail(String reportEmail)
    {
        this.reportEmail = reportEmail;
    }

    /**
     * Specifies if we should send emails using MX records or the
     * outgoing mail server.
     *
     * @return true to use MX records, false to use outgoing mail
     * server.
     */
    public boolean isUseMxRecords()
    {
        return useMxRecords;
    }

    public void setUseMxRecords(boolean useMxRecords)
    {
        this.useMxRecords = useMxRecords;
    }

    /**
     * The SMTP mail host used to send internal reports and error
     * emails.
     *
     * @return host name or ip address of SMTP server.
     */
    public String getSmtpHost()
    {
        if (null == smtpHost) {
            return "";
        } else {
            return smtpHost;
        }
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
     * @return true if should use TLS when availaable.
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
     * The <code>From</code> address for mail sent by the Untangle
     * Platform.
     *
     * @return <code>From</code> email address
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
     * User for SMTP auth. If <code>user</code> or <code>pass</code>
     * is null, don't use SMTP auth.
     *
     * @return user name for SMTP auth.
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
     * The Password to use for SMTP Auth.  If null or if the user is
     * null, don't use SMTP auth.
     *
     * @return a <code>String</code> giving the password for SMTP Auth
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
     * The Local host name for sending (SMTP HELO).  If null, use the
     * actual local host name.
     *
     * @return the local host name for HELO.
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
