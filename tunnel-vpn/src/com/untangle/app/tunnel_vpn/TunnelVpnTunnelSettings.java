/**
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

import java.net.InetAddress;
import java.util.List;
import java.util.LinkedList;

import com.untangle.uvm.app.IPMatcher;

@SuppressWarnings("serial")
public class TunnelVpnTunnelSettings implements java.io.Serializable
{
    private boolean enabled = true;
    private String name;

    private boolean allTraffic = false;
    private List<String> tags = new LinkedList<String>();
    private List<IPMatcher> hosts = new LinkedList<IPMatcher>();
    
    public TunnelVpnTunnelSettings() {}

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    public String getName() { return name; }
    public void setName(String name) { this.name = ( name == null ? null : name.replaceAll("\\s","") ); }

    public boolean getAllTraffic() { return allTraffic; }
    public void setAllTraffic(boolean newValue) { allTraffic = newValue; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> newValue) { tags = newValue; }

    public List<IPMatcher> getHosts() { return hosts; }
    public void setHosts(List<IPMatcher> newValue) { hosts = newValue; }
}
