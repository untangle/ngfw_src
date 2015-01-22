/**
 * $Id$
 */

package com.untangle.node.capture;

import java.io.Serializable;
import java.net.InetAddress;

@SuppressWarnings("serial")
public class CaptureUserEntry implements Serializable
{
    private InetAddress userAddress;
    private String userName;
    private Boolean isAnonymous;
    private long sessionCreation;
    private long sessionActivity;
    private long sessionCounter;

    public CaptureUserEntry() {}
    
    public CaptureUserEntry(InetAddress userAddress, String userName, Boolean isAnonymous)
    {
        this.userAddress = userAddress;
        this.userName = userName;
        this.isAnonymous = isAnonymous;
        sessionCreation = System.currentTimeMillis();
        sessionActivity = sessionCreation;
    }

    public InetAddress getUserAddress() { return userAddress; }
    public void setUserAddress( InetAddress newValue ) { this.userAddress = newValue; }
    
    public String getUserName() { return userName; }
    public void setUserName( String newValue ) { this.userName = newValue; }

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
}
