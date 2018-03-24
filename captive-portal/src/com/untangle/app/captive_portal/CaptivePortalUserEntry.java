/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.io.Serializable;
import org.json.JSONString;

/**
 * This is the implementation of a captive portal user entry used to track
 * authenticated users.
 * 
 * @author mahotz
 */

@SuppressWarnings("serial")
public class CaptivePortalUserEntry implements Serializable, JSONString
{
    private String userAddress;
    private String userName;
    private Boolean isAnonymous;
    private long sessionCreation;
    private long sessionActivity;
    private long sessionCounter;

    public CaptivePortalUserEntry()
    {
    }

    public CaptivePortalUserEntry(String userAddress, String userName, Boolean isAnonymous)
    {
        this.userAddress = userAddress;
        this.userName = userName;
        this.isAnonymous = isAnonymous;
        sessionCreation = System.currentTimeMillis();
        sessionActivity = sessionCreation;
    }

// THIS IS FOR ECLIPSE - @formatter:off

    public String getUserAddress() { return userAddress; }
    public void setUserAddress( String newValue ) { this.userAddress = newValue; }

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

// THIS IS FOR ECLIPSE - @formatter:on

    public void updateActivityTimer()
    {
        sessionActivity = System.currentTimeMillis();
        sessionCounter++;
    }

    public String toString()
    {
        String local = ("NAME: " + userName + " ADDR:" + userAddress);
        return (local);
    }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
