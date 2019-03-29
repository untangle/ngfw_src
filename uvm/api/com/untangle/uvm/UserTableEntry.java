/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.Objects;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;

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
    private long        lastSessionTime = 0; /* time of the last new session */

    private long quotaSize = 0; /* the quota size - 0 means no quota assigned */
    private long quotaRemaining = 0; /* the quota remaining */
    private long quotaIssueTime = 0; /* the issue time on the quota */
    private long quotaExpirationTime = 0; /* the expiration time on the assigned quota */

    private HashMap<String,Tag> tags = new HashMap<>();
    
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
        this.setLastAccessTime( other.getLastAccessTime() );
        this.setLastSessionTime( other.getLastSessionTime() );
        this.setTags( other.getTags() );
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

    public long getLastSessionTime() { return this.lastSessionTime; }
    public void setLastSessionTime( long newValue )
    {
        if ( newValue == this.lastSessionTime )
            return;
        //updateEvent( "lastSessionTime", String.valueOf(this.lastSessionTime), String.valueOf(newValue) );
        this.lastSessionTime = newValue;
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

    public synchronized List<Tag> getTags()
    {
        removeExpiredTags();
        return new LinkedList<>(this.tags.values());
    }
    
    public synchronized void setTags( List<Tag> newValue )
    {
        HashMap<String,Tag> newSet = new HashMap<>();
        if ( newValue != null ) {
            for ( Tag t : newValue ) {
                if ( t == null || t.getName() == null )
                    continue;
                newSet.put(t.getName(),t);
            }
        }
        updateEvent( "tags", Tag.tagsToString(this.tags.values()), Tag.tagsToString(newSet.values()) );
        this.tags = newSet;
        updateAccessTime();
    }

    public synchronized String getTagsString()
    {
        return Tag.tagsToString( getTags() );
    }

    public synchronized void addTag( Tag tag )
    {
        if ( tag == null || tag.getName() == null )
            return;
        updateEvent( "addTag", "", tag.getName() );
        this.tags.put( tag.getName(), tag );
    }

    public synchronized void addTags( List<Tag> tags )
    {
        if ( tags == null )
            return;
        for ( Tag tag : tags ) {
            addTag( tag );
        }
    }

    public synchronized Tag removeTag( Tag tag )
    {
        Tag t = this.tags.remove( tag.getName() );
        if ( t != null )
            updateEvent( "removeTag", "", t.getName() );
        return t;
    }

    public synchronized boolean hasTag( String name )
    {
        Tag t = this.tags.get( name );
        if ( t == null )
            return false;
        if ( t.isExpired() ) {
            this.tags.remove( t.getName() );
            return false;
        }
        return true;
    }

    public void removeExpiredTags()
    {
        for ( Iterator<Tag> i = this.tags.values().iterator() ; i.hasNext() ; ) {
            Tag t = i.next();
            if ( t.isExpired() )
                i.remove();
        }
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

        UserTableEvent event = new UserTableEvent( this.username, key, newValue, oldValue );
        UvmContextFactory.context().logEvent(event);
    }
    
}
