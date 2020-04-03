/**
 * $Id: DeviceTableEntry.java 42014 2015-12-29 22:25:51Z dmorris $
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.Objects;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * This is a host table entry
 * It stores the address, and a table of all the known information about this host (attachments)
 */
@SuppressWarnings("serial")
public class DeviceTableEntry implements Serializable, JSONString
{
    private static final Logger logger = Logger.getLogger(DeviceTableEntry.class);

    /**
     * MAC address is all lower case
     * 11:00:aa:bb:cc:99
     */
    private String      macAddress;
    private String      macVendor = null;
    private String      username = null;
    private String      hostname = null;
    private String      hostnameLastKnown = null;
    private String      httpUserAgent = null;
    private int         interfaceId = 0;
    private long        lastSessionTime = 0; /* time of the last new session */

    private HashMap<String,Tag> tags = new HashMap<>();
    
    private static final String IPV4_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private static Pattern ipv4Pattern = Pattern.compile( IPV4_PATTERN );

    /**
     * if logChanges is true, changes to this object will log events
     * This exists so that we don't log events when instantiation from JSON happens
     * After instantiation is complete, this is set to true and future changes are logged
     */
    private boolean     logChanges = false;
    
    public DeviceTableEntry() {}

    public DeviceTableEntry( String macAddress )
    {
        enableLogging(); //since this was instantiated by hand, enabling logging
        setMacAddress( macAddress );
    }

    public void copy( DeviceTableEntry other )
    {
        this.setMacAddress( other.getMacAddress() );
        this.setMacVendor( other.getMacVendor() );
        this.setUsername( other.getUsername() );
        this.setHostname( other.getHostname() );
        this.setHostnameLastKnown( other.getHostnameLastKnown() );
        this.setHttpUserAgent( other.getHttpUserAgent() );
        this.setLastSessionTime( other.getLastSessionTime() );
        this.setInterfaceId( other.getInterfaceId() );
        this.setTags( other.getTags() );
    }
    
    public String getMacAddress() { return this.macAddress; }
    public void setMacAddress( String newValue )
    {
        if ( Objects.equals( newValue, this.macAddress ) )
            return;
        this.macAddress = newValue;
        updateEvent( "macAddress", this.macAddress, newValue );
    }

    public long getLastSessionTime() { return this.lastSessionTime; }
    public void setLastSessionTime( long newValue )
    {
        if ( newValue == this.lastSessionTime )
            return;
        //updateEvent( "lastSessionTime", String.valueOf(this.lastSessionTime), String.valueOf(newValue) );
        this.lastSessionTime = newValue;
    }
    
    public int getInterfaceId() { return this.interfaceId; }
    public void setInterfaceId( int newValue )
    {
        if ( newValue == this.interfaceId )
            return;
        updateEvent( "interfaceId", Integer.valueOf(this.interfaceId).toString(), Integer.valueOf(newValue).toString() );
        this.interfaceId = newValue;
    }
    
    public String getMacVendor() { return this.macVendor; }
    public void setMacVendor( String newValue )
    {
        if ( Objects.equals( newValue, this.macVendor ) )
            return;
        updateEvent( "macVendor", this.macVendor, newValue );
        this.macVendor = newValue;
    }

    public String getHostname() { return this.hostname; }
    public void setHostname( String newValue )
    {
        if ( newValue != null ) {
            Matcher matcher = ipv4Pattern.matcher( newValue );
            if (matcher.matches()) {
                return; // if its an IP, ignore it
            }
        }

        if ( Objects.equals( newValue, this.hostname ) )
            return;
        updateEvent( "hostname", this.hostname, newValue );
        this.hostname = newValue;
    }

    public String getHostnameLastKnown() { return this.hostnameLastKnown; }
    public void setHostnameLastKnown( String newValue )
    {
        if ( newValue != null ) {
            Matcher matcher = ipv4Pattern.matcher( newValue );
            if (matcher.matches()) {
                return; // if its an IP, ignore it
            }
        }

        if ( Objects.equals( newValue, this.hostnameLastKnown ) )
            return;
        updateEvent( "hostnameLastKnown", this.hostnameLastKnown, newValue );
        this.hostnameLastKnown = newValue;
    }

    public String getHttpUserAgent() { return this.httpUserAgent; }
    public void setHttpUserAgent( String newValue )
    {
        // check hashcodes are equal which may be a bit quicker
        if ( newValue != null && this.httpUserAgent != null && newValue.hashCode() == this.httpUserAgent.hashCode() )
            return;
        if ( Objects.equals( newValue, this.httpUserAgent ) )
            return;
        updateEvent( "httpUserAgent", String.valueOf(this.httpUserAgent), String.valueOf(newValue) );
        this.httpUserAgent = newValue;
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
    
    public String getUsername()
    {
        if ( "".equals(this.username) )
             return null;
        return this.username;
    }
    
    public void setUsername( String newValue )
    {
        if ( "".equals(newValue) )
            newValue = null;
        if ( Objects.equals( newValue, this.username ) )
            return;
        updateEvent( "username", this.username, newValue );
        this.username = newValue;
    }

    /* 13.0 Deprecated */
    public String getDeviceUsername() { return null; }
    public void setDeviceUsername( String newValue )
    {
        setUsername( newValue );
    }
    
    /**
     * Utility method to check that hostname is known
     */
    public boolean hostnameKnown()
    {
        String hostname = getHostname();
        if (hostname == null)
            return false;
        if (hostname.equals(""))
            return false;
        return true;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public static String sanitizeMacAddress( String macAddr )
    {
        if ( macAddr == null )
            return null;
        String tmp = macAddr.toLowerCase().replaceAll("[^0-9a-f:]","");
        if ( tmp.length() != 17 ) {
            logger.warn("Invalid MAC Address: " + macAddr, new Exception());
            return null;
        }
        return tmp;
    }
    
    public void enableLogging()
    {
        this.logChanges = true;
    }

    private void updateEvent( String key, String oldValue, String newValue )
    {
        if ( !logChanges )
            return;
        if ( this.macAddress == null )
            return;
        if ( newValue == null ) 
            newValue = "null";

        DeviceTableEvent event = new DeviceTableEvent( this, this.macAddress, key, newValue, oldValue );
        UvmContextFactory.context().logEvent(event);
    }
}
