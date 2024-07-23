/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import com.untangle.uvm.util.ValidSerializable;

/**
 * Device settings.
 */
@SuppressWarnings("serial")
@ValidSerializable
public class DeviceSettings implements Serializable, JSONString
{
    private String deviceName;

    public static enum Duplex { AUTO,
            M10000_FULL_DUPLEX, M10000_HALF_DUPLEX,
            M1000_FULL_DUPLEX, M1000_HALF_DUPLEX,
            M100_FULL_DUPLEX, M100_HALF_DUPLEX,
            M10_FULL_DUPLEX, M10_HALF_DUPLEX };
    
    private Duplex duplex = Duplex.AUTO; 

    private Integer mtu; /* null means auto */

    private Boolean energyEfficientEthernet = false;
    
    public String getDeviceName() { return this.deviceName; }
    public void setDeviceName( String newValue ) { this.deviceName = newValue; }

    public Duplex getDuplex() { return this.duplex; }
    public void setDuplex( Duplex newValue ) { this.duplex = newValue; }

    public Integer getMtu() { return this.mtu; }
    public void setMtu( Integer newValue ) { this.mtu = newValue; }

    public Boolean getEnergyEfficientEthernet() { return this.energyEfficientEthernet; }
    public void setEnergyEfficientEthernet( Boolean newValue ) { this.energyEfficientEthernet = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}