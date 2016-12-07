/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

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
    private static final int LICENSE_TRAFFIC_AGE_MAX_TIME = 60 * 60 * 1000; /* 60 minutes */
    private static final Logger logger = Logger.getLogger(HostTableEntry.class);

    private InetAddress address = null;
    private String      macAddress = null;
    private String      macVendor = null;
    private int         interfaceId = 0;
    private long        creationTime = 0;
    private long        lastAccessTime = 0;
    private long        lastSessionTime = 0; /* time of the last new session */
    private long        lastCompletedTcpSessionTime = 0; /* time of the last completed TCP session */
    private boolean     entitled = true;

    private String hostname = null;

    private boolean captivePortalAuthenticated = false; /* marks if this user is authenticated with captive portal */

    private String usernameCapture = null;
    private String usernameTunnel = null;
    private String usernameOpenvpn = null;
    private String usernameAdConnector = null;
    private String usernameDevice = null;
    
    private boolean penaltyBoxed = false;
    private long    penaltyBoxExitTime = 0;
    private long    penaltyBoxEntryTime = 0;

    private long quotaSize = 0; /* the quota size - 0 means no quota assigned */
    private long quotaRemaining = 0; /* the quota remaining */
    private long quotaIssueTime = 0; /* the issue time on the quota */
    private long quotaExpirationTime = 0; /* the expiration time on the assigned quota */

    private String httpUserAgent = null; /* the user-agent header from HTTP */

    public HostTableEntry()
    {
        creationTime = System.currentTimeMillis();
        updateAccessTime();
    }

    public void copy( HostTableEntry other )
    {
        this.setAddress( other.getAddress() );
        this.setMacAddress( other.getMacAddress() );
        this.setInterfaceId( other.getInterfaceId() );
        this.setCreationTime( other.getCreationTime() );
        this.setLastAccessTime( other.getLastAccessTime() );
        this.setLastSessionTime( other.getLastSessionTime() );
        this.setLastCompletedTcpSessionTime( other.getLastCompletedTcpSessionTime() );
        this.setEntitled( other.getEntitled() );
        this.setHostname( other.getHostname() );
        this.setUsernameAdConnector( other.getUsernameAdConnector() );
        this.setUsernameCapture( other.getUsernameCapture() );
        this.setCaptivePortalAuthenticated( other.getCaptivePortalAuthenticated() );
        this.setUsernameTunnel( other.getUsernameTunnel() );
        this.setUsernameOpenvpn( other.getUsernameOpenvpn() );
        this.setPenaltyBoxed( other.getPenaltyBoxed() );
        this.setPenaltyBoxExitTime( other.getPenaltyBoxExitTime() );
        this.setPenaltyBoxEntryTime( other.getPenaltyBoxEntryTime() );
        this.setQuotaSize( other.getQuotaSize() );
        this.setQuotaRemaining( other.getQuotaRemaining() );
        this.setQuotaIssueTime( other.getQuotaIssueTime() );
        this.setQuotaExpirationTime( other.getQuotaExpirationTime() );
        this.setHttpUserAgent( other.getHttpUserAgent() );
    }
    
    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress newValue )
    {
        if ( Objects.equals( newValue, this.address ) )
            return;
        updateEvent( "address", (this.address!=null?this.address.getHostAddress():"null"), (newValue!=null?newValue.getHostAddress():"null") );
        this.address = newValue;
        updateAccessTime();
    }

    public String getMacAddress() { return this.macAddress; }
    public void setMacAddress( String newValue )
    {
        if ( Objects.equals( newValue, this.macAddress ) )
            return;
        updateEvent( "macAddress", this.macAddress, newValue );
        this.macAddress = newValue;
        updateAccessTime();
    }

    public String getMacVendor() { return this.macVendor; }
    public void setMacVendor( String newValue )
    {
        if ( Objects.equals( newValue, this.macVendor ) )
            return;
        updateEvent( "macVendor", this.macVendor, newValue);
        this.macVendor = newValue;
        updateAccessTime();
    }
    
    public int getInterfaceId() { return this.interfaceId; }
    public void setInterfaceId( int newValue )
    {
        if ( newValue == this.interfaceId )
            return;
        updateEvent( "interfaceId", (new Integer(this.interfaceId)).toString(), new Integer(newValue).toString() );
        this.interfaceId = newValue;
        updateAccessTime();
    }
    
    public long getCreationTime() { return this.creationTime; }
    public void setCreationTime( long newValue )
    {
        if ( newValue == this.creationTime )
            return;
        updateEvent( "creationTime", String.valueOf(this.creationTime), String.valueOf(newValue) );
        this.creationTime = newValue;
        updateAccessTime();
    }

    public long getLastAccessTime() { return this.lastAccessTime; }
    public void setLastAccessTime( long newValue )
    {
        if ( newValue == this.lastAccessTime )
            return;
        //updateEvent( "lastAccessTime", String.valueOf(this.lastAccessTime), String.valueOf(newValue) );
        this.lastAccessTime = newValue;
        updateAccessTime();
    }

    public long getLastSessionTime() { return this.lastSessionTime; }
    public void setLastSessionTime( long newValue )
    {
        if ( newValue == this.lastSessionTime )
            return;
        //updateEvent( "lastSessionTime", String.valueOf(this.lastSessionTime), String.valueOf(newValue) );
        this.lastSessionTime = newValue;
        updateAccessTime();
    }

    public long getLastCompletedTcpSessionTime() { return this.lastCompletedTcpSessionTime; }
    public void setLastCompletedTcpSessionTime( long newValue )
    {
        if ( newValue == this.lastCompletedTcpSessionTime )
            return;
        //updateEvent( "lastCompletedTcpSessionTime", String.valueOf(this.lastCompletedTcpSessionTime), String.valueOf(newValue) );
        this.lastCompletedTcpSessionTime = newValue;
        updateAccessTime();
    }
    
    public boolean getEntitled() { return this.entitled; }
    public void setEntitled( boolean newValue )
    {
        if ( newValue == this.entitled )
            return;
        updateEvent( "entitled", this.hostname, (newValue ? "true" : "false") );
        this.entitled = newValue;
        updateAccessTime();
    }
    
    public String getHostname() { return this.hostname; }
    public void setHostname( String newValue )
    {
        if ( Objects.equals( newValue, this.hostname ) )
            return;
        updateEvent( "hostname", this.hostname, newValue );
        this.hostname = newValue;
        updateAccessTime();
    }

    public String getUsernameAdConnector() { return this.usernameAdConnector; }
    public void setUsernameAdConnector( String newValue )
    {
        newValue = (newValue == null ? null : newValue.toLowerCase());

        if ( Objects.equals( newValue, this.usernameAdConnector ) )
            return;
        updateEvent( "usernameAdConnector", this.usernameAdConnector, newValue );
        this.usernameAdConnector = newValue;
        updateAccessTime();
    }
    
    public String getUsernameCapture() { return this.usernameCapture; }
    public void setUsernameCapture( String newValue )
    {
        newValue = (newValue == null ? null : newValue.toLowerCase());

        if ( Objects.equals( newValue, this.usernameCapture ) )
            return;
        updateEvent( "usernameCapture", this.usernameCapture, newValue );
        this.usernameCapture = newValue;
        updateAccessTime();
    }

    public String getUsernameDevice() { return this.usernameDevice; }
    public void setUsernameDevice( String newValue )
    {
        newValue = (newValue == null ? null : newValue.toLowerCase());

        if ( Objects.equals( newValue, this.usernameDevice ) )
            return;
        updateEvent( "usernameDevice", this.usernameDevice, newValue);
        this.usernameDevice = newValue;
        updateAccessTime();
    }
    
    public boolean getCaptivePortalAuthenticated() { return this.captivePortalAuthenticated; }
    public void setCaptivePortalAuthenticated( boolean newValue )
    {
        if ( newValue == this.captivePortalAuthenticated )
            return;
        updateEvent( "captivePortalAuthenticated", String.valueOf(this.captivePortalAuthenticated), String.valueOf(newValue) );
        this.captivePortalAuthenticated = newValue;
        updateAccessTime();
    }

    public String getUsernameTunnel() { return this.usernameTunnel; }
    public void setUsernameTunnel( String newValue )
    {
        newValue = (newValue == null ? null : newValue.toLowerCase());

        if ( Objects.equals( newValue, this.usernameTunnel ) )
            return;
        updateEvent( "usernameTunnel", this.usernameTunnel, newValue );
        this.usernameTunnel = newValue;
        updateAccessTime();
    }

    public String getUsernameOpenvpn() { return this.usernameOpenvpn; }
    public void setUsernameOpenvpn( String newValue )
    {
        newValue = (newValue == null ? null : newValue.toLowerCase());

        if ( Objects.equals( newValue, this.usernameOpenvpn ) )
            return;
        updateEvent( "usernameOpenvpn", this.usernameOpenvpn, newValue );
        this.usernameOpenvpn = newValue;
        updateAccessTime();
    }

    public boolean getPenaltyBoxed() { return this.penaltyBoxed; }
    public void setPenaltyBoxed( boolean newValue )
    {
        if ( newValue == this.penaltyBoxed )
            return;
        updateEvent( "penaltyBoxed", String.valueOf(this.penaltyBoxed), String.valueOf(newValue) );
        this.penaltyBoxed = newValue;
        updateAccessTime();
    }

    public long getPenaltyBoxExitTime() { return this.penaltyBoxExitTime; }
    public void setPenaltyBoxExitTime( long newValue )
    {
        if ( newValue == this.penaltyBoxExitTime )
            return;
        updateEvent( "penaltyBoxExitTime", String.valueOf(this.penaltyBoxExitTime), String.valueOf(newValue) );
        this.penaltyBoxExitTime = newValue;
        updateAccessTime();
    }

    public long getPenaltyBoxEntryTime() { return this.penaltyBoxEntryTime; }
    public void setPenaltyBoxEntryTime( long newValue )
    {
        if ( newValue == this.penaltyBoxEntryTime )
            return;
        updateEvent( "penaltyBoxEntryTime", String.valueOf(this.penaltyBoxEntryTime), String.valueOf(newValue) );
        this.penaltyBoxEntryTime = newValue;
        updateAccessTime();
    }

    public long getQuotaSize() { return this.quotaSize; }
    public void setQuotaSize( long newValue )
    {
        if ( newValue == this.quotaSize )
            return;
        updateEvent( "quotaSize", String.valueOf(this.quotaSize), String.valueOf(newValue) );
        this.quotaSize = newValue;
        updateAccessTime();
    }

    public long getQuotaRemaining() { return this.quotaRemaining; }
    public void setQuotaRemaining( long newValue )
    {
        if ( newValue == this.quotaRemaining )
            return;
        //updateEvent( "quotaRemaining", String.valueOf(this.quotaRemaining), String.valueOf(newValue) );
        this.quotaRemaining = newValue;
        updateAccessTime();
    }

    public long getQuotaIssueTime() { return this.quotaIssueTime; }
    public void setQuotaIssueTime( long newValue )
    {
        if ( newValue == this.quotaIssueTime )
            return;
        updateEvent( "quotaIssueTime", String.valueOf(this.quotaIssueTime), String.valueOf(newValue) );
        this.quotaIssueTime = newValue;
        updateAccessTime();
    }

    public long getQuotaExpirationTime() { return this.quotaExpirationTime; }
    public void setQuotaExpirationTime( long newValue )
    {
        if ( newValue == this.quotaExpirationTime )
            return;
        updateEvent( "quotaExpirationTime", String.valueOf(this.quotaExpirationTime), String.valueOf(newValue) );
        this.quotaExpirationTime = newValue;
        updateAccessTime();
    }
    
    public String getHttpUserAgent() { return this.httpUserAgent; }
    public void setHttpUserAgent( String newValue )
    {
        if ( Objects.equals( newValue, this.httpUserAgent ) )
            return;
        updateEvent( "httpUserAgent", String.valueOf(this.httpUserAgent), String.valueOf(newValue) );
        this.httpUserAgent = newValue;
        updateAccessTime();
    }

    /**
     * Get the "best" username of all known sources
     * Precedence defined below
     */
    public String getUsername()
    {
        if (getUsernameCapture() != null)
            return getUsernameCapture();
        if (getUsernameTunnel() != null)
            return getUsernameTunnel();
        if (getUsernameOpenvpn() != null)
            return getUsernameOpenvpn();
        if (getUsernameAdConnector() != null)
            return getUsernameAdConnector();
        if (getUsernameDevice() != null)
            return getUsernameDevice();
        return null;
    }

    /**
     * Get the source of the "best" username
     */
    public String getUsernameSource()
    {
        if (getUsernameCapture() != null)
            return "Captive Portal";
        if (getUsernameTunnel() != null)
            return "L2TP/IPsec";
        if (getUsernameOpenvpn() != null)
            return "OpenVPN";
        if (getUsernameAdConnector() != null)
            return "Directory Connector";
        if (getUsernameDevice() != null)
            return "Device";
        return null;
    }

    /**
     * This returns the "active" status for purposes of licensing
     * Only "active" hosts are counted against licenses while many
     * inactive hosts can be in the host table.
     */
    public boolean getActive()
    {
        long cutoffTime = System.currentTimeMillis() - LICENSE_TRAFFIC_AGE_MAX_TIME;
        if ( getLastCompletedTcpSessionTime() > cutoffTime )
            return true;

        return false;
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

    private void updateEvent( String key, String oldValue, String newValue )
    {
        if ( this.address == null ) {
            logger.warn("updateEvent will null address: " + oldValue + " -> " + newValue );
            return;
        }
        if ( newValue == null ) 
            newValue = "null";

        HostTableEvent event = new HostTableEvent( this.address, key, newValue );
        UvmContextFactory.context().logEvent(event);
    }
    
}
