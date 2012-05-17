/**
 * $Id: ReportingUser.java,v 1.00 2012/05/14 12:58:47 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.Serializable;

import org.apache.commons.codec.binary.Base64;

import com.untangle.uvm.PasswordUtil;

@SuppressWarnings("serial")
public class ReportingUser implements Serializable
{
    private String emailAddress;
    private boolean emailSummaries;
    private byte[] passwordHash = null;
    private boolean onlineAccess;

    public ReportingUser() {}

    public String getEmailAddress() { return this.emailAddress; }
    public void setEmailAddress( String emailAddress ) { this.emailAddress = emailAddress; }

    public boolean getEmailSummaries() { return this.emailSummaries; }
    public void setEmailSummaries( boolean emailSummaries ) { this.emailSummaries = emailSummaries; }

    public boolean getOnlineAccess() { return this.onlineAccess; }
    public void setOnlineAccess( boolean onlineAccess ) { this.onlineAccess = onlineAccess; }

    public byte[] trans_getPasswordHash() { return this.passwordHash; }
    public void setPasswordHash( byte[] passwordHash ) { this.passwordHash = passwordHash; }

    public String getPassword() { return null; }
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


}

    