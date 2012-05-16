/**
 * $Id: ReportingUser.java,v 1.00 2012/05/14 12:58:47 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ReportingUser implements Serializable
{
    private String emailAddress;
    private boolean emailSummaries;
    private String password;
    private boolean onlineAccess;

    public ReportingUser() {}

    public String getEmailAddress() { return this.emailAddress; }
    public void setEmailAddress( String emailAddress ) { this.emailAddress = emailAddress; }

    public boolean getEmailSummaries() { return this.emailSummaries; }
    public void setEmailSummaries( boolean emailSummaries ) { this.emailSummaries = emailSummaries; }

    public String getPassword() { return this.password; }
    public void setPassword( String password ) { this.password = password; }
    
    public boolean getOnlineAccess() { return this.onlineAccess; }
    public void setOnlineAccess( boolean onlineAccess ) { this.onlineAccess = onlineAccess; }
}

    