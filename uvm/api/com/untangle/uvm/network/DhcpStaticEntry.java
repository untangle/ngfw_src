/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.util.SafeCheck;
import com.untangle.uvm.util.SafeType;

/**
 * Dhcp static entry.
 */
@SuppressWarnings("serial")
public class DhcpStaticEntry implements Serializable, JSONString
{
    // Flows into /etc/dnsmasq.d/dhcp-static as `dhcp-host={mac},{ip}` and
    // `# {description}` comment. dnsmasq supports `dhcp-script=` exec directive,
    // so newline injection in either field is RCE.
    @SafeCheck(SafeType.MAC_ADDRESS)
    private String macAddress;
    private InetAddress address;
    @SafeCheck(SafeType.SIMPLE_TEXT)
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