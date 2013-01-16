/**
 * $Id: NetworkSettings.java 32043 2012-05-31 21:31:47Z dmorris $
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.network.PortForwardRule;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.StaticRoute;

/**
 * Network settings.
 */
@SuppressWarnings("serial")
public class NetworkSettings implements Serializable, JSONString
{
    private List<InterfaceSettings> interfaces = null;
    private List<PortForwardRule> portForwardRules = null;
    private List<NatRule> natRules = null;
    private List<BypassRule> bypassRules = null;
    private List<StaticRoute> staticRoutes = null;

    public NetworkSettings() { }

    public List<InterfaceSettings> getInterfaces() { return this.interfaces; }
    public void setInterfaces( List<InterfaceSettings> interfaces ) { this.interfaces = interfaces; }
    
    public List<PortForwardRule> getPortForwardRules() { return this.portForwardRules; }
    public void setPortForwardRules( List<PortForwardRule> portForwardRules ) { this.portForwardRules = portForwardRules; }

    public List<NatRule> getNatRules() { return this.natRules; }
    public void setNatRules( List<NatRule> natRules ) { this.natRules = natRules; }

    public List<BypassRule> getBypassRules() { return this.bypassRules; }
    public void setBypassRules( List<BypassRule> bypassRules ) { this.bypassRules = bypassRules; }

    public List<StaticRoute> getStaticRoutes() { return this.staticRoutes; }
    public void setStaticRoutes( List<StaticRoute> staticRoutes ) { this.staticRoutes = staticRoutes; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
