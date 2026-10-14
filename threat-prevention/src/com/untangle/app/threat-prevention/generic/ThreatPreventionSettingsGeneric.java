/**
 * $Id$
 */
package com.untangle.app.threat_prevention.generic;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.app.threat_prevention.ThreatPreventionRule;
import com.untangle.app.threat_prevention.ThreatPreventionSettings;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.generic.RuleGeneric;

/**
 * Generic (V2) settings for the Threat Prevention app, consumed by the Vue UI.
 */
@SuppressWarnings("serial")
public class ThreatPreventionSettingsGeneric implements Serializable, JSONString {
    private Integer reputationThreshold = 20;
    
    private LinkedList<RuleGeneric> threat_prevention_rules = new LinkedList<>();
    private LinkedList<GenericRule> passSites = new LinkedList<>();

    private Boolean customBlockPageEnabled = false;
    private String customBlockPageUrl = "";
    private Boolean closeHttpsBlockEnabled = false;

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public Integer getReputationThreshold() { return reputationThreshold; }
    public void setReputationThreshold(Integer reputationThreshold) { this.reputationThreshold = reputationThreshold; }

    public LinkedList<RuleGeneric> getThreat_prevention_rules() { return threat_prevention_rules;}
    public void setThreat_prevention_rules(LinkedList<RuleGeneric> threat_prevention_rules) { this.threat_prevention_rules = threat_prevention_rules; }

    public LinkedList<GenericRule> getPassSites() { return passSites; }
    public void setPassSites(LinkedList<GenericRule> passSites) { this.passSites = passSites; }

    public Boolean getCustomBlockPageEnabled() { return customBlockPageEnabled; }
    public void setCustomBlockPageEnabled(Boolean customBlockPageEnabled) { this.customBlockPageEnabled = customBlockPageEnabled; }
    
    public String getCustomBlockPageUrl() { return customBlockPageUrl; }
    public void setCustomBlockPageUrl(String customBlockPageUrl) { this.customBlockPageUrl = customBlockPageUrl; }

    public Boolean getCloseHttpsBlockEnabled() { return closeHttpsBlockEnabled; }
    public void setCloseHttpsBlockEnabled(Boolean closeHttpsBlockEnabled) { this.closeHttpsBlockEnabled = closeHttpsBlockEnabled; }

    /**
     * Transforms this V2 generic settings back into a V1 ThreatPreventionSettings object.
     * Mutates the passed-in v1 object in place so that V1-only fields (e.g. version)
     * are preserved. Used by setSettingsV2().
     *
     * @param v1 deep-cloned V1 settings (mutated in place)
     * @return the same v1 reference, populated from this V2 object
     */
    public ThreatPreventionSettings transformGenericToThreatPreventionSettings(ThreatPreventionSettings v1) {
        if (v1 == null) v1 = new ThreatPreventionSettings();
        v1.setReputationThreshold(this.reputationThreshold);
        v1.setRules(ThreatPreventionRule.transformGenericToThreatPreventionRules(this.threat_prevention_rules, v1.getRules()));
        v1.setPassSites(this.passSites != null ? new LinkedList<>(this.passSites) : new LinkedList<>());
        v1.setCustomBlockPageEnabled(this.customBlockPageEnabled);
        v1.setCustomBlockPageUrl(this.customBlockPageUrl);
        v1.setCloseHttpsBlockEnabled(this.closeHttpsBlockEnabled);
        return v1;
    }

}
