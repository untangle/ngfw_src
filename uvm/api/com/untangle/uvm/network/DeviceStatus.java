/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import com.untangle.uvm.util.ValidSerializable;

/**
 * This object represents the current status/config of an device.
 * This is not a settings object.
 */
@SuppressWarnings("serial")
@ValidSerializable
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

    private Integer mtu;

    private Boolean eeeEnabled = false;
    private Boolean eeeActive = false;

    private String supportedLinkModes;

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

    public Integer getMtu( ) { return this.mtu; }
    public void setMtu( Integer newValue ) { this.mtu = newValue; }

    public Boolean getEeeEnabled( ) { return this.eeeEnabled; }
    public void setEeeEnabled( Boolean newValue ) { this.eeeEnabled = newValue; }

    public Boolean getEeeActive( ) { return this.eeeActive; }
    public void setEeeActive( Boolean newValue ) { this.eeeActive = newValue; }

    public String getSupportedLinkModes( ) { return this.supportedLinkModes; }
    public void setSupportedLinkModes( String newValue ) { this.supportedLinkModes = newValue; }


}
