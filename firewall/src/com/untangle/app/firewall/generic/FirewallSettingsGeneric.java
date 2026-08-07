/**
 * $Id$
 */
package com.untangle.app.firewall.generic;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.app.firewall.FirewallRule;
import com.untangle.app.firewall.FirewallSettings;
import com.untangle.uvm.generic.RuleGeneric;

/**
 * Generic (V2) settings for the Firewall app, consumed by the Vue UI.
 * Transforms the rules list into the shared RuleGeneric shape.
 */
@SuppressWarnings("serial")
public class FirewallSettingsGeneric implements Serializable, JSONString {

    private LinkedList<RuleGeneric> firewall_rules = new LinkedList<>();

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public LinkedList<RuleGeneric> getFirewall_rules() { return firewall_rules; }
    public void setFirewall_rules(LinkedList<RuleGeneric> firewall_rules) { this.firewall_rules = firewall_rules; }

    /**
     * Transforms this V2 generic settings back into a V1 FirewallSettings object.
     * Mutates the passed-in v1 object in place so that V1-only fields (e.g. version)
     * are preserved. Used by setSettingsV2().
     *
     * @param v1 deep-cloned V1 settings (mutated in place)
     * @return the same v1 reference, populated from this V2 object
     */
    public FirewallSettings transformGenericToFirewallSettings(FirewallSettings v1)
    {
        if (v1 == null) v1 = new FirewallSettings();
        v1.setRules(FirewallRule.transformGenericToFirewallRules(this.firewall_rules, v1.getRules()));
        return v1;
    }
}
