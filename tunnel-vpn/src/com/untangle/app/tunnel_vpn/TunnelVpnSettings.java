/**
 * $Id$
 */

package com.untangle.app.tunnel_vpn;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for the TunnelVpn app.
 */
@SuppressWarnings("serial")
public class TunnelVpnSettings implements Serializable, JSONString
{
    private Integer version = Integer.valueOf(1);

    private List<TunnelVpnTunnelSettings> servers = new LinkedList<TunnelVpnTunnelSettings>();

    private List<TunnelVpnRule> rules = new LinkedList<TunnelVpnRule>();
    
    public TunnelVpnSettings() {}

    public Integer getVersion() { return this.version; }
    public void setVersion(Integer newValue) { this.version = newValue; }

    public List<TunnelVpnTunnelSettings> getTunnels() { return this.servers; }
    public void setTunnels( List<TunnelVpnTunnelSettings> newValue ) { this.servers = newValue; }

    public List<TunnelVpnRule> getRules() { return rules; }
    public void setRules(List<TunnelVpnRule> newValue) { rules = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
