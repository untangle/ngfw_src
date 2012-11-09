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

    private String hostname;
    private String usernameAdConnector;
    private String usernameCapture;

    private boolean penaltyBoxed = false;
    private long    penaltyBoxExitTime;
    private long    penaltyBoxEntryTime;
    private int     penaltyBoxPriority;

    private long quotaSize; /* the quota size - 0 means no quota assigned */
    private long quotaRemaining; /* the quota remaining */
    private long quotaIssueTime; /* the issue time on the quota */
    private long quotaExpirationTime; /* the expiration time on the assigned quota */

    private String httpUserAgent; /* the user-agent header from HTTP */
    private String httpUserAgentOs; /* the os part of the user-agent header from HTTP */
    private long   httpUserAgentSetDate; /* date the httpUserAgent was set */
    
    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress address ) { this.address = address; updateAccessTime(); }
    
    public long getCreationTime() { return this.creationTime; }
    public void setCreationTime( long creationTime ) { this.creationTime = creationTime; updateAccessTime(); }

    public long getLastAccessTime() { return this.lastAccessTime; }
    public void setLastAccessTime( long lastAccessTime ) { this.lastAccessTime = lastAccessTime; updateAccessTime(); }

    public String getHostname() { return this.hostname; }
    public void setHostname( String hostname ) { this.hostname = hostname; updateAccessTime(); }

    public String getUsernameAdConnector() { return this.usernameAdConnector; }
    public void setUsernameAdConnector( String usernameAdConnector ) { this.usernameAdConnector = usernameAdConnector; updateAccessTime(); }

    public String getUsernameCapture() { return this.usernameCapture; }
    public void setUsernameCapture( String usernameCapture ) { this.usernameCapture = usernameCapture; updateAccessTime(); }
    
    public boolean getPenaltyBoxed() { return this.penaltyBoxed; }
    public void setPenaltyBoxed( boolean penaltyBoxed ) { this.penaltyBoxed = penaltyBoxed; updateAccessTime(); }

    public long getPenaltyBoxExitTime() { return this.penaltyBoxExitTime; }
    public void setPenaltyBoxExitTime( long penaltyBoxExitTime ) { this.penaltyBoxExitTime = penaltyBoxExitTime; updateAccessTime(); }

    public long getPenaltyBoxEntryTime() { return this.penaltyBoxEntryTime; }
    public void setPenaltyBoxEntryTime( long penaltyBoxEntryTime ) { this.penaltyBoxEntryTime = penaltyBoxEntryTime; updateAccessTime(); }

    public int getPenaltyBoxPriority() { return this.penaltyBoxPriority; }
    public void setPenaltyBoxPriority( int penaltyBoxPriority ) { this.penaltyBoxPriority = penaltyBoxPriority; updateAccessTime(); }

    public long getQuotaSize() { return this.quotaSize; }
    public void setQuotaSize( long quotaSize ) { this.quotaSize = quotaSize; updateAccessTime(); }

    public long getQuotaRemaining() { return this.quotaRemaining; }
    public void setQuotaRemaining( long quotaRemaining ) { this.quotaRemaining = quotaRemaining; updateAccessTime(); }

    public long getQuotaIssueTime() { return this.quotaIssueTime; }
    public void setQuotaIssueTime( long quotaIssueTime ) { this.quotaIssueTime = quotaIssueTime; updateAccessTime(); }

    public long getQuotaExpirationTime() { return this.quotaExpirationTime; }
    public void setQuotaExpirationTime( long quotaExpirationTime ) { this.quotaExpirationTime = quotaExpirationTime; updateAccessTime(); }
    
    public String getHttpUserAgent() { return this.httpUserAgent; }
    public void setHttpUserAgent( String httpUserAgent ) { this.httpUserAgent = httpUserAgent; updateAccessTime(); this.httpUserAgentSetDate = System.currentTimeMillis(); }

    public String getHttpUserAgentOs() { return this.httpUserAgentOs; }
    public void setHttpUserAgentOs( String httpUserAgentOs ) { this.httpUserAgentOs = httpUserAgentOs; updateAccessTime(); this.httpUserAgentSetDate = System.currentTimeMillis(); }

    public long getHttpUserAgentSetDate() { return this.httpUserAgentSetDate; }
    public void setHttpUserAgentSetDate( long httpUserAgentSetDate ) { this.httpUserAgentSetDate = httpUserAgentSetDate; updateAccessTime(); }
    
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
