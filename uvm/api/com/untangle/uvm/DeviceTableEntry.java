/**
 * $Id: DeviceTableEntry.java 42014 2015-12-29 22:25:51Z dmorris $
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.net.InetAddress;
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
    private String      deviceUsername = null;
    private String      hostname = null;
    private String      httpUserAgent = null;
    
    private long        lastSeenTime = 0;

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
        String tmp = sanitizeMacAddress(macAddress);
        if ( tmp == null )
            throw new RuntimeException("Invalid MAC: " + macAddress );
        this.macAddress = tmp;

        enableLogging(); //since this was instantiated by hand, enabling logging
    }

    public void copy( DeviceTableEntry other )
    {
        this.setMacAddress( other.getMacAddress() );
        this.setMacVendor( other.getMacVendor() );
        this.setDeviceUsername( other.getDeviceUsername() );
        this.setHostname( other.getHostname() );
        this.setHttpUserAgent( other.getHttpUserAgent() );
        this.setLastSeenTime( other.getLastSeenTime() );
    }
    
    public String getMacAddress() { return this.macAddress; }
    public void setMacAddress( String newValue ) { this.macAddress = newValue; }

    public long getLastSeenTime() { return this.lastSeenTime; }
    public void setLastSeenTime( long newValue ) { this.lastSeenTime = newValue; }

    public String getMacVendor() { return this.macVendor; }
    public void setMacVendor( String newValue )
    {
        updateEvent("macVendor",this.macVendor,newValue);
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

        updateEvent("hostname",this.hostname,newValue);
        this.hostname = newValue;
    }

    public String getHttpUserAgent() { return this.httpUserAgent; }
    public void setHttpUserAgent( String newValue )
    {
        updateEvent("httpUserAgent",String.valueOf(this.httpUserAgent),String.valueOf(newValue));
        this.httpUserAgent = newValue;
    }
    
    public String getDeviceUsername()
    {
        if ( "".equals(this.deviceUsername) )
             return null;
        return this.deviceUsername;
    }
    
    public void setDeviceUsername( String newValue )
    {
        if ( "".equals(newValue) )
            newValue = null;
        updateEvent("deviceUsername",this.deviceUsername,newValue);
        this.deviceUsername = newValue;
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

    public void updateLastSeenTime()
    {
        this.lastSeenTime = System.currentTimeMillis();
    }

    private void updateEvent( String key, String oldValue, String newValue )
    {
        if ( !logChanges )
            return;
        if ( this.macAddress == null )
            return;
        if ( oldValue == null && newValue == null ) //no change
            return;
        if ( newValue == null ) 
            newValue = "null";
        if ( newValue.equals(oldValue) ) // no change
            return;

        DeviceTableEvent event = new DeviceTableEvent( this, this.macAddress, key, newValue );
        UvmContextFactory.context().logEvent(event);
    }
    
}
