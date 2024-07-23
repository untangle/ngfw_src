/**
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

import java.util.List;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import com.untangle.uvm.util.ValidSerializable;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This in the implementation of a TunnelVpn Rule
 *
 * A rule is basically a collection of TunnelVpnRuleConditions
 * and what to do if the conditions match
 */
@SuppressWarnings("serial")
@ValidSerializable
public class TunnelVpnRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<TunnelVpnRuleCondition> conditions;

    private Integer ruleId;
    private boolean enabled = true;
    private boolean ipv6Enabled = true;
    private int tunnelId = -1;
    private String description;
    
    public TunnelVpnRule() { }

    public TunnelVpnRule(boolean enabled, boolean ipv6Enabled, List<TunnelVpnRuleCondition> conditions, int tunnelId, String description)
    {
        this.setConditions(conditions);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setIpv6Enabled(Boolean.valueOf(ipv6Enabled));
        this.setTunnelId(tunnelId);
        this.setDescription(description);
    }
    
    public List<TunnelVpnRuleCondition> getConditions() { return this.conditions; }
    public void setConditions( List<TunnelVpnRuleCondition> conditions ) { this.conditions = conditions; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public boolean getEnabled() { return enabled; }
    public void setEnabled( boolean enabled ) { this.enabled = enabled; }

    public boolean getIpv6Enabled() { return ipv6Enabled; }
    public void setIpv6Enabled( boolean ipv6Enabled ) { this.ipv6Enabled = ipv6Enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public int getTunnelId() { return tunnelId; }
    public void setTunnelId( int newDestination ) { this.tunnelId = newDestination; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
