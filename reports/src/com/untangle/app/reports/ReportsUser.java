/**
 * $Id$
 */
package com.untangle.app.reports;

import java.io.Serializable;

import java.util.List;
import java.util.LinkedList;

import org.apache.commons.codec.binary.Base64;

import com.untangle.uvm.PasswordUtil;

import org.json.JSONString;
import org.json.JSONObject;

/**
 * ReportsUser settings.
 */
@SuppressWarnings("serial")
public class ReportsUser implements Serializable, JSONString
{
    private String emailAddress;
    private boolean emailAlerts = false;
    private boolean emailSummaries = true;
    private byte[] passwordHash = null;
    private boolean onlineAccess;
    private List<Integer> emailTemplateIds = new LinkedList<>();

    public ReportsUser() {}

    public String getEmailAddress() { return this.emailAddress; }
    public void setEmailAddress( String newValue ) { this.emailAddress = newValue; }

    public boolean getEmailAlerts() { return this.emailAlerts; }
    public void setEmailAlerts( boolean newValue ) { this.emailAlerts = newValue; }

    public boolean getEmailSummaries() { return this.emailSummaries; }
    public void setEmailSummaries( boolean newValue ) { this.emailSummaries = newValue; }

    public boolean getOnlineAccess() { return this.onlineAccess; }
    public void setOnlineAccess( boolean newValue ) { this.onlineAccess = newValue; }

    public byte[] trans_getPasswordHash() { return this.passwordHash; }
    public void setPasswordHash( byte[] newValue ) { this.passwordHash = newValue; }

    public List<Integer> getEmailTemplateIds() { return this.emailTemplateIds; }
    public void setEmailTemplateIds( List<Integer> newValue ) { this.emailTemplateIds = newValue; }

    public String getPassword()
    {
        return null;
    }

    public void setPassword( String password )
    {
        if ( password == null )
            return;
        if ( "".equals(password) )
            return;
        this.passwordHash = PasswordUtil.encrypt(password);
    }

    public String getPasswordHashBase64()
    {
        if ( this.passwordHash == null || "".equals(this.passwordHash) )
            return "";
        else
            return new String(Base64.encodeBase64(passwordHash));
    }

    public void setPasswordHashBase64( String passwordHashBase64 )
    {
        if ( passwordHashBase64 == null )
            return;
        if ( "".equals( passwordHashBase64 ) )
            return;
        this.passwordHash = Base64.decodeBase64(passwordHashBase64.getBytes());
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
