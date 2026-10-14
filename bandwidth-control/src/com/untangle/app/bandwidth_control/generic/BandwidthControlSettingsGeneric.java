/**
 * $Id$
 */
package com.untangle.app.bandwidth_control.generic;

import com.untangle.app.bandwidth_control.BandwidthControlSettings;

import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Generic (V2) settings for the Bandwidth Control app, consumed by the Vue UI.
 * Transforms only the rules list; passes configured through unchanged.
 */
@SuppressWarnings("serial")
public class BandwidthControlSettingsGeneric implements Serializable, JSONString {

    private Boolean configured;
    private LinkedList<BandwidthControlRuleGeneric> bandwidth_control_rules = new LinkedList<>();

    public Boolean getConfigured() { return configured; }
    public void setConfigured(Boolean configured) { this.configured = configured; }

    public LinkedList<BandwidthControlRuleGeneric> getBandwidth_control_rules() { return bandwidth_control_rules; }
    public void setBandwidth_control_rules(LinkedList<BandwidthControlRuleGeneric> bandwidth_control_rules) { this.bandwidth_control_rules = bandwidth_control_rules; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms this V2 settings object into V1 by mutating the passed-in
     * V1 settings object. Preserves any V1-only fields (e.g. settingsVersion) not exposed in V2.
     *
     * @param v1 current V1 settings (mutated in place)
     * @return the same v1 reference, populated from this V2 object
     */
    public BandwidthControlSettings transformGenericToBandwidthControlSettings(BandwidthControlSettings v1) {
        if (v1 == null) v1 = new BandwidthControlSettings();
        if (this.configured != null) v1.setConfigured(this.configured);
        if (this.bandwidth_control_rules != null)
            v1.setRules(BandwidthControlRuleGeneric.transformGenericToRules(this.bandwidth_control_rules, v1.getRules()));
        return v1;
    }
}
