/**
 * $Id: DeviceTableEntry.java 42014 2015-12-29 22:25:51Z dmorris $
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
public class DeviceTableEntry implements Serializable, JSONString
{
    private static final Logger logger = Logger.getLogger(DeviceTableEntry.class);

    /**
     * MAC address is all lower case
     * 11:00:aa:bb:cc:99
     */
    private final String macAddress;
    
    private long        lastSeenTime = 0;

    public DeviceTableEntry( String macAddress )
    {
        String tmp = sanitizeMacAddress(macAddress);
        if ( tmp == null )
            throw new RuntimeException("Invalid MAC: " + macAddress );
        this.macAddress = tmp;
    }

    public String getMacAddress() { return this.macAddress; }

    public long getLastSeenTime() { return this.lastSeenTime; }
    public void setLastSeenTime( long newValue ) { this.lastSeenTime = newValue; }

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
        if ( tmp.length() != 18 ) {
            logger.warn("Invalid MAC Address: " + macAddr, new Exception());
            return null;
        }
        return tmp;
    }
    
    private void updateLastSeenTime()
    {
        this.lastSeenTime = System.currentTimeMillis();
    }

    private void updateEvent( String key, String oldValue, String newValue )
    {
        /* FIXME */
    }
    
}
