/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * UVM mail settings.
 */
@SuppressWarnings("serial")
public class MailSettings implements Serializable, JSONString
{
    private String fromAddress;
    private boolean useMxRecords = true;
    private String smtpHost;
    private int smtpPort = 25;
    private String authUser;
    private String authPass;

    public MailSettings() {}
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
    /**
     * Specifies if we should send emails using MX records or the
     * outgoing mail server.
     */
    public boolean isUseMxRecords() { return useMxRecords; }
    public void setUseMxRecords(boolean useMxRecords) { this.useMxRecords = useMxRecords; }

    /**
     * The SMTP mail host used to send internal reports and error
     * emails.
     */
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    /**
     * The port used to connect to the SMTP mail host.  If the SMTP_HOST
     * is null, this is ignored.
     */
    public int getSmtpPort() { return smtpPort; }
    public void setSmtpPort(int smtpPort) { this.smtpPort = smtpPort; }

    /**
     * The <code>From</code> address for mail sent by the Untangle
     * Platform.
     */
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    /**
     * User for SMTP auth. If <code>user</code> or <code>pass</code>
     * is null, don't use SMTP auth.
     */
    public String getAuthUser() { return authUser; }
    public void setAuthUser(String authUser) { this.authUser = authUser; }

    /**
     * The Password to use for SMTP Auth.  If null or if the user is
     * null, don't use SMTP auth.
     */
    public String getAuthPass() { return authPass; }
    public void setAuthPass(String authPass) { this.authPass = authPass; }
}
