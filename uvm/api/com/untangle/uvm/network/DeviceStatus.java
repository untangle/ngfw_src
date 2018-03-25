/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * This object represents the current status/config of an device.
 * This is not a settings object.
 */
@SuppressWarnings("serial")
public class DeviceStatus implements Serializable, JSONString
{
    private String deviceName;

    private String macAddress;
    
    public static enum ConnectedStatus { CONNECTED, DISCONNECTED, UNKNOWN, MISSING };
    private ConnectedStatus connected;

    private Integer mbit; /* 10, 100, 1000, null(unknown) */

    public static enum DuplexStatus { FULL_DUPLEX, HALF_DUPLEX, UNKNOWN };
    private DuplexStatus duplex; 
    
    private String vendor;
    
    public DeviceStatus() {}
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
    public String getDeviceName( ) { return this.deviceName; }
    public void setDeviceName( String newValue ) { this.deviceName = newValue; }

    public String getMacAddress( ) { return this.macAddress; }
    public void setMacAddress( String newValue ) { this.macAddress = newValue; }

    public ConnectedStatus getConnected( ) { return this.connected; }
    public void setConnected( ConnectedStatus newValue ) { this.connected = newValue; }

    public Integer getMbit( ) { return this.mbit; }
    public void setMbit( Integer newValue ) { this.mbit = newValue; }

    public DuplexStatus getDuplex( ) { return this.duplex; }
    public void setDuplex( DuplexStatus newValue ) { this.duplex = newValue; }
    
    public String getVendor( ) { return this.vendor; }
    public void setVendor( String newValue ) { this.vendor = newValue; }
}
