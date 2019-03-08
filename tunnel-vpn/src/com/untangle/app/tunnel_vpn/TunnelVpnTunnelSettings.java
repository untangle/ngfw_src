/**
 * $Id$
 */

package com.untangle.app.tunnel_vpn;

import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Manages the settings for a tunnel
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class TunnelVpnTunnelSettings implements JSONString, Serializable
{
    private Integer tunnelId = null;
    private boolean enabled = true;
    private String name;
    private boolean nat = true;

    private String provider = null;
    private String username = null;
    private String password = null;

    private Integer boundInterfaceId = null; /* 0 and null mean any interface */
    
// THIS IS FOR ECLIPSE - @formatter:off
    
    public TunnelVpnTunnelSettings() {}

    public Integer getTunnelId() { return this.tunnelId; }
    public void setTunnelId( Integer newValue ) { this.tunnelId = newValue; }

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    public String getName() { return name; }
    public void setName(String name) { this.name = ( name == null ? null : name.replaceAll("\\s","") ); }

    public String getProvider() { return provider; }
    public void setProvider(String newValue) { this.provider = newValue; }

    public String getUsername() { return username; }
    public void setUsername(String newValue) { this.username = newValue; }

    public String getPassword() { return password; }
    public void setPassword(String newValue) { this.password = newValue; }

    public boolean getNat() { return nat; }
    public void setNat(boolean newValue) { this.nat = newValue; }

    public Integer getBoundInterfaceId() { return this.boundInterfaceId; }
    public void setBoundInterfaceId( Integer newValue ) { this.boundInterfaceId = newValue; }
    
// THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
