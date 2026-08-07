/**
 * $Id$
 */
package com.untangle.app.firewall;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.app.firewall.generic.FirewallSettingsGeneric;

/**
 * Settings for the Firewall app.
 */
@SuppressWarnings("serial")
public class FirewallSettings implements Serializable, JSONString
{
    private Integer version = Integer.valueOf(1);
    private List<FirewallRule> rules = null;
    
    public FirewallSettings()
    {
        this.rules = new LinkedList<>();
    }

    public FirewallSettings(List<FirewallRule> rules)
    {
        this.rules = rules;
    }
    
    public Integer getVersion() { return this.version; }
    public void setVersion(Integer newValue) { this.version = newValue; }

    public List<FirewallRule> getRules() { return rules; }
    public void setRules( List<FirewallRule> newValue ) { this.rules = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms this V1 FirewallSettings into the generic V2 representation
     * for the Vue UI. Used by getSettingsV2().
     *
     * @return FirewallSettingsGeneric populated from this V1 object
     */
    public FirewallSettingsGeneric transformFirewallSettingsToGeneric()
    {
        FirewallSettingsGeneric g = new FirewallSettingsGeneric();
        g.setFirewall_rules(FirewallRule.transformFirewallRulesToGeneric(this.rules));
        return g;
    }
}
