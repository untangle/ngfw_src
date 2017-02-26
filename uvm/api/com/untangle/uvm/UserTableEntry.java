/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.Objects;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * This is a host table entry
 * It stores the username, and a table of all the known information about this host (attachments)
 */
@SuppressWarnings("serial")
public class UserTableEntry implements Serializable, JSONString
{
    private static final Logger logger = Logger.getLogger(UserTableEntry.class);

    private String      username = null;
    private long        creationTime = 0;
    private long        lastAccessTime = 0;
    private long        lastSeenTime = 0;

    private long quotaSize = 0; /* the quota size - 0 means no quota assigned */
    private long quotaRemaining = 0; /* the quota remaining */
    private long quotaIssueTime = 0; /* the issue time on the quota */
    private long quotaExpirationTime = 0; /* the expiration time on the assigned quota */

    public UserTableEntry()
    {
        creationTime = System.currentTimeMillis();
        updateAccessTime();
    }

    public void copy( UserTableEntry other )
    {
        this.setUsername( other.getUsername() );
        this.setCreationTime( other.getCreationTime() );
        this.setQuotaSize( other.getQuotaSize() );
        this.setQuotaRemaining( other.getQuotaRemaining() );
        this.setQuotaIssueTime( other.getQuotaIssueTime() );
        this.setQuotaExpirationTime( other.getQuotaExpirationTime() );
    }
    
    public String getUsername() { return this.username; }
    public void setUsername( String newValue )
    {
        if ( Objects.equals( newValue, this.username ) )
            return;
        updateEvent( "username", (this.username!=null?this.username:"null"), (newValue!=null?newValue:"null") );
        this.username = newValue;
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

    public long getLastSeenTime() { return this.lastSeenTime; }
    public void setLastSeenTime( long newValue )
    {
        if ( newValue == this.lastSeenTime )
            return;
        this.lastSeenTime = newValue;
        //updateEvent( "lastSeenTime", this.lastSeenTime, newValue );
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
        if ( this.username == null ) {
            //logger.warn("updateEvent with null username: " + oldValue + " -> " + newValue );
            return;
        }
        if ( newValue == null ) 
            newValue = "null";

        UserTableEvent event = new UserTableEvent( this.username, key, newValue );
        UvmContextFactory.context().logEvent(event);
    }
    
}
