/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import com.untangle.uvm.util.ValidSerializable;

/**
 * Dhcp static entry.
 */
@SuppressWarnings("serial")
@ValidSerializable
public class DhcpStaticEntry implements Serializable, JSONString
{
    private String macAddress;
    private InetAddress address;
    private String description;
    
    public DhcpStaticEntry( String macAddress, InetAddress address)
    {
        this.macAddress = macAddress;
        this.address = address;
    }

    public DhcpStaticEntry() {}

    public String getMacAddress() { return this.macAddress; }
    public void setMacAddress( String newValue ) { this.macAddress = newValue; }

    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress newValue ) { this.address = newValue; }

    public String getDescription() { return this.description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}