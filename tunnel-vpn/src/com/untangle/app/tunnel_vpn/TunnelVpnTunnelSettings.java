/**
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

import java.net.InetAddress;
import java.util.List;
import java.util.LinkedList;

@SuppressWarnings("serial")
public class TunnelVpnTunnelSettings implements java.io.Serializable
{
    private Integer tunnelId = null;
    private boolean enabled = true;
    private String name;

    private String provider = null;
    private String username = null;
    private String password = null;
    
    private boolean allTraffic = false;
    private List<String> tags = new LinkedList<String>();
    
    public TunnelVpnTunnelSettings() {}

    public Integer getTunnelId() { return this.tunnelId; }
    public void setTunnelId( Integer newValue ) { this.tunnelId = newValue; }

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    public String getName() { return name; }
    public void setName(String name) { this.name = ( name == null ? null : name.replaceAll("\\s","") ); }

    public boolean getAllTraffic() { return allTraffic; }
    public void setAllTraffic(boolean newValue) { allTraffic = newValue; }

    public String getProvider() { return provider; }
    public void setProvider(String newValue) { this.provider = newValue; }

    public String getUsername() { return username; }
    public void setUsername(String newValue) { this.username = newValue; }

    public String getPassword() { return password; }
    public void setPassword(String newValue) { this.password = newValue; }
    
    public List<String> getTags() { return tags; }
    public void setTags(List<String> newValue) { tags = newValue; }
}
