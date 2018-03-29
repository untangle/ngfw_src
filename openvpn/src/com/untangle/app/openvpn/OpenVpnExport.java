/**
 * $Id$
 */
package com.untangle.app.openvpn;

import com.untangle.uvm.app.IPMaskedAddress;

/**
 * A list of network exports for remote clients
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class OpenVpnExport implements java.io.Serializable, org.json.JSONString
{
    private boolean enabled = true;
    private String name;
    private IPMaskedAddress network;

    public OpenVpnExport()
    {
    }

// THIS IS FOR ECLIPSE - @formatter:off

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }
    
    public String getName() { return name; }
    public void setName( String newValue ) { this.name = newValue; }

    public IPMaskedAddress getNetwork() { return this.network; }
    public void setNetwork( IPMaskedAddress newValue ) { this.network = newValue; }

// THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
