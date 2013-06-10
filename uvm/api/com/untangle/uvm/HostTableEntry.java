/**
 * $Id: HostTableEntry.java,v 1.00 2012/11/08 13:03:51 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * This is a host table entry
 * It stores the address, and a table of all the known information about this host (attachments)
 */
@SuppressWarnings("serial")
public class HostTableEntry implements Serializable, JSONString
{
    private final Logger logger = Logger.getLogger(getClass());

    private InetAddress address;
    private long        creationTime;
    private long        lastAccessTime;
    private long        lastSessionTime; /* time of the last new session */

    private String hostname;
    private String usernameAdConnector;
    private String usernameCapture;

    private boolean penaltyBoxed = false;
    private long    penaltyBoxExitTime;
    private long    penaltyBoxEntryTime;

    private long quotaSize; /* the quota size - 0 means no quota assigned */
    private long quotaRemaining; /* the quota remaining */
    private long quotaIssueTime; /* the issue time on the quota */
    private long quotaExpirationTime; /* the expiration time on the assigned quota */

    private String httpUserAgent; /* the user-agent header from HTTP */
    private String httpUserAgentOs; /* the os part of the user-agent header from HTTP */
    private long   httpUserAgentSetDate; /* date the httpUserAgent was set */
    
    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress newValue ) { this.address = newValue; updateAccessTime(); }
    
    public long getCreationTime() { return this.creationTime; }
    public void setCreationTime( long newValue ) { this.creationTime = newValue; updateAccessTime(); }

    public long getLastAccessTime() { return this.lastAccessTime; }
    public void setLastAccessTime( long newValue ) { this.lastAccessTime = newValue; updateAccessTime(); }

    public long getLastSessionTime() { return this.lastSessionTime; }
    public void setLastSessionTime( long newValue ) { this.lastSessionTime = newValue; updateAccessTime(); }
    
    public String getHostname() { return this.hostname; }
    public void setHostname( String newValue ) { this.hostname = newValue; updateAccessTime(); }

    public String getUsernameAdConnector() { return this.usernameAdConnector; }
    public void setUsernameAdConnector( String newValue ) { this.usernameAdConnector = newValue; updateAccessTime(); }

    public String getUsernameCapture() { return this.usernameCapture; }
    public void setUsernameCapture( String newValue ) { this.usernameCapture = newValue; updateAccessTime(); }
    
    public boolean getPenaltyBoxed() { return this.penaltyBoxed; }
    public void setPenaltyBoxed( boolean newValue ) { this.penaltyBoxed = newValue; updateAccessTime(); }

    public long getPenaltyBoxExitTime() { return this.penaltyBoxExitTime; }
    public void setPenaltyBoxExitTime( long newValue ) { this.penaltyBoxExitTime = newValue; updateAccessTime(); }

    public long getPenaltyBoxEntryTime() { return this.penaltyBoxEntryTime; }
    public void setPenaltyBoxEntryTime( long newValue ) { this.penaltyBoxEntryTime = newValue; updateAccessTime(); }

    public long getQuotaSize() { return this.quotaSize; }
    public void setQuotaSize( long newValue ) { this.quotaSize = newValue; updateAccessTime(); }

    public long getQuotaRemaining() { return this.quotaRemaining; }
    public void setQuotaRemaining( long newValue ) { this.quotaRemaining = newValue; updateAccessTime(); }

    public long getQuotaIssueTime() { return this.quotaIssueTime; }
    public void setQuotaIssueTime( long newValue ) { this.quotaIssueTime = newValue; updateAccessTime(); }

    public long getQuotaExpirationTime() { return this.quotaExpirationTime; }
    public void setQuotaExpirationTime( long newValue ) { this.quotaExpirationTime = newValue; updateAccessTime(); }
    
    public String getHttpUserAgent() { return this.httpUserAgent; }
    public void setHttpUserAgent( String newValue ) { this.httpUserAgent = newValue; updateAccessTime(); this.httpUserAgentSetDate = System.currentTimeMillis(); }

    public String getHttpUserAgentOs() { return this.httpUserAgentOs; }
    public void setHttpUserAgentOs( String newValue ) { this.httpUserAgentOs = newValue; updateAccessTime(); this.httpUserAgentSetDate = System.currentTimeMillis(); }

    public long getHttpUserAgentSetDate() { return this.httpUserAgentSetDate; }
    public void setHttpUserAgentSetDate( long newValue ) { this.httpUserAgentSetDate = newValue; updateAccessTime(); }
    
    public String getUsername()
    {
        if (getUsernameCapture() != null)
            return getUsernameCapture();
        if (getUsernameAdConnector() != null)
            return getUsernameAdConnector();
        return null;
    }

    public String getUsernameSource()
    {
        if (getUsernameCapture() != null)
            return "Captive Portal";
        if (getUsernameAdConnector() != null)
            return "Directory Connector";
        return null;
    }

    /**
     * Utility method to check that hostname is known
     * Its not enough to just check that its null or ""
     * because it will be set to the IP address string repr by default
     */
    public boolean isHostnameKnown()
    {
        String hostname = getHostname();
        if (hostname == null)
            return false;
        if (hostname.equals(""))
            return false;
        if (getAddress() == null) {
            logger.warn("null address");
            return true;
        }
        if (hostname.equals(getAddress().getHostAddress()))
            return false;
        return true;
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    private void updateAccessTime()
    {
        this.lastAccessTime = System.currentTimeMillis();
    }
}
