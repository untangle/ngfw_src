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

/**
 * Network settings.
 */
@SuppressWarnings("serial")
public class NetworkSettings implements Serializable, JSONString
{
    List<PortForwardRule> portForwards = new LinkedList<PortForwardRule>();
    List<NatRule> natRules = new LinkedList<NatRule>();

    public NetworkSettings() { }

    public List<PortForwardRule> getPortForwards() { return this.portForwards; }
    public void setPortForwards( List<PortForwardRule> portForwards ) { this.portForwards = portForwards; }

    public List<NatRule> getNatRules() { return this.natRules; }
    public void setNatRules( List<NatRule> natRules ) { this.natRules = natRules; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
