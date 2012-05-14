/**
 * $Id: ReportingUser.java,v 1.00 2012/05/14 12:58:47 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ReportingUser implements Serializable
{
    private String username;
    private String passwordHash;
    private String emailAddress;
    private boolean sendEmail;
    private boolean onlineAccess;

    public ReportingUser() {}

    public String getUsername() { return this.username; }
    public void setUsername( String username ) { this.username = username; }

    public String getPasswordHash() { return this.passwordHash; }
    public void setPasswordHash( String passwordHash ) { this.passwordHash = passwordHash; }

    public String getEmailAddress() { return this.emailAddress; }
    public void setEmailAddress( String emailAddress ) { this.emailAddress = emailAddress; }
    
    public boolean getSendEmail() { return this.sendEmail; }
    public void setSendEmail( boolean sendEmail ) { this.sendEmail = sendEmail; }

    public boolean getOnlineAccess() { return this.onlineAccess; }
    public void setOnlineAccess( boolean onlineAccess ) { this.onlineAccess = onlineAccess; }
}

    