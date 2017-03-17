/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.io.Serializable;
import java.net.InetAddress;

@SuppressWarnings("serial")
public class CaptivePortalUserEntry implements Serializable
{
    private InetAddress userNetAddress;
    private String userMacAddress;
    private String userName;
    private Boolean isAnonymous;
    private Boolean isMacLogin;
    private long sessionCreation;
    private long sessionActivity;
    private long sessionCounter;

    public CaptivePortalUserEntry() {}

    public CaptivePortalUserEntry(InetAddress userNetAddress, String userMacAddress, String userName, Boolean isAnonymous)
    {
        this.userNetAddress = userNetAddress;
        this.userMacAddress = userMacAddress;
        this.userName = userName;
        this.isAnonymous = isAnonymous;
        this.isMacLogin = false;
        sessionCreation = System.currentTimeMillis();
        sessionActivity = sessionCreation;
    }

    public InetAddress getUserNetAddress() { return userNetAddress; }
    public void setUserNetAddress( InetAddress newValue ) { this.userNetAddress = newValue; }

    public String getUserMacAddress() { return userMacAddress; }
    public void setUserMacAddress( String newValue ) { this.userMacAddress = newValue; }

    public String getUserName() { return userName; }
    public void setUserName( String newValue ) { this.userName = newValue; }

    public Boolean getMacLogin() { return isMacLogin; }
    public void setMacLogin( Boolean newValue ) { this.isMacLogin = newValue; }

    public Boolean getAnonymous() { return isAnonymous; }
    public void setAnonymous( Boolean newValue ) { this.isAnonymous = newValue; }

    public long getSessionCreation() { return sessionCreation; }
    public void setSessionCreation( long newValue ) { this.sessionCreation = newValue; }

    public long getSessionActivity() { return sessionActivity; }
    public void setSessionActivity( long newValue ) { this.sessionActivity = newValue; }

    public long getSessionCounter() { return sessionCounter; }
    public void setSessionCounter( long newValue ) { this.sessionCounter = newValue; }

    public void updateActivityTimer()
    {
        sessionActivity = System.currentTimeMillis();
        sessionCounter++;
    }

    public String toString()
    {
        String local = ("ADDR:" + userNetAddress.getHostAddress().toString() + " MAC:" + userMacAddress + " NAME:" + userName);
        return(local);
    }
}
