/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention.generic;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.untangle.app.intrusion_prevention.IntrusionPreventionDaySchedule;
import com.untangle.app.intrusion_prevention.IntrusionPreventionSettings;
import com.untangle.app.intrusion_prevention.IntrusionPreventionSignature;
import com.untangle.app.intrusion_prevention.IntrusionPreventionVariable;
import com.untangle.uvm.generic.RuleGeneric;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Generic (V2) settings for the Intrusion Prevention app, consumed by the Vue UI.
 * Passes through all non-rule fields unchanged; transforms only the ip_rules list
 * into the shared RuleGeneric shape.
 */
@SuppressWarnings("serial")
public class IntrusionPreventionSettingsGeneric implements Serializable, JSONString {

    private Integer version;

    private LinkedList<IntrusionPreventionRuleGeneric> ip_rules = new LinkedList<>();

    // Pass-through fields
    private List<IntrusionPreventionSignature> signatures;
    private List<IntrusionPreventionVariable> variables;
    private LinkedList<RuleGeneric> bypassRules;
    private Integer iptablesNfqNumber;
    private Integer iptablesMaxScanSize;
    private String iptablesProcessing;
    private String blockAction;
    private JSONObject suricataSettings;
    private String updateSignatureFrequency;
    private List<IntrusionPreventionDaySchedule> updateSignatureSchedule;
    private IntrusionPreventionDaySchedule updateSignatureWeekly;

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public LinkedList<IntrusionPreventionRuleGeneric> getIp_rules() { return ip_rules; }
    public void setIp_rules(LinkedList<IntrusionPreventionRuleGeneric> ip_rules) { this.ip_rules = ip_rules; }

    public List<IntrusionPreventionSignature> getSignatures() { return signatures; }
    public void setSignatures(List<IntrusionPreventionSignature> signatures) { this.signatures = signatures; }

    public List<IntrusionPreventionVariable> getVariables() { return variables; }
    public void setVariables(List<IntrusionPreventionVariable> variables) { this.variables = variables; }

    public LinkedList<RuleGeneric> getBypassRules() { return bypassRules; }
    public void setBypassRules(LinkedList<RuleGeneric> bypassRules) { this.bypassRules = bypassRules; }

    public Integer getIptablesNfqNumber() { return iptablesNfqNumber; }
    public void setIptablesNfqNumber(Integer iptablesNfqNumber) { this.iptablesNfqNumber = iptablesNfqNumber; }

    public Integer getIptablesMaxScanSize() { return iptablesMaxScanSize; }
    public void setIptablesMaxScanSize(Integer iptablesMaxScanSize) { this.iptablesMaxScanSize = iptablesMaxScanSize; }

    public String getIptablesProcessing() { return iptablesProcessing; }
    public void setIptablesProcessing(String iptablesProcessing) { this.iptablesProcessing = iptablesProcessing; }

    public String getBlockAction() { return blockAction; }
    public void setBlockAction(String blockAction) { this.blockAction = blockAction; }

    public JSONObject getSuricataSettings() { return suricataSettings; }
    public void setSuricataSettings(JSONObject suricataSettings) { this.suricataSettings = suricataSettings; }

    public String getUpdateSignatureFrequency() { return updateSignatureFrequency; }
    public void setUpdateSignatureFrequency(String updateSignatureFrequency) { this.updateSignatureFrequency = updateSignatureFrequency; }

    public List<IntrusionPreventionDaySchedule> getUpdateSignatureSchedule() { return updateSignatureSchedule; }
    public void setUpdateSignatureSchedule(List<IntrusionPreventionDaySchedule> updateSignatureSchedule) { this.updateSignatureSchedule = updateSignatureSchedule; }

    public IntrusionPreventionDaySchedule getUpdateSignatureWeekly() { return updateSignatureWeekly; }
    public void setUpdateSignatureWeekly(IntrusionPreventionDaySchedule updateSignatureWeekly) { this.updateSignatureWeekly = updateSignatureWeekly; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms this V2 settings object into V1 by mutating the passed-in
     * V1 settings object. Preserves any V1-only fields not exposed in V2.
     *
     * @param v1 current V1 settings (mutated in place)
     * @return the same v1 reference, populated from this V2 object
     */
    public IntrusionPreventionSettings transformGenericToIntrusionPreventionSettings(IntrusionPreventionSettings v1) {
        if (v1 == null) v1 = new IntrusionPreventionSettings();

        if (this.version != null) v1.setVersion(this.version);
        if (this.signatures != null) v1.setSignatures(this.signatures);
        if (this.variables != null) v1.setVariables(this.variables);
        if (this.bypassRules != null)
            v1.setBypassRules(RuleGeneric.transformGenericToLegacyBypassRules(this.bypassRules, v1.getBypassRules()));
        if (this.iptablesNfqNumber != null) v1.setIptablesNfqNumber(this.iptablesNfqNumber);
        if (this.iptablesMaxScanSize != null) v1.setIptablesMaxScanSize(this.iptablesMaxScanSize);
        if (this.iptablesProcessing != null) v1.setIptablesProcessing(this.iptablesProcessing);
        if (this.blockAction != null) v1.setBlockAction(this.blockAction);
        if (this.suricataSettings != null) v1.setSuricataSettings(this.suricataSettings);
        if (this.updateSignatureFrequency != null) v1.setUpdateSignatureFrequency(this.updateSignatureFrequency);
        if (this.updateSignatureSchedule != null) v1.setUpdateSignatureSchedule(this.updateSignatureSchedule);
        if (this.updateSignatureWeekly != null) v1.setUpdateSignatureWeekly(this.updateSignatureWeekly);

        if (this.ip_rules != null)
            v1.setRules(IntrusionPreventionRuleGeneric.transformGenericToIpRules(this.ip_rules, v1.getRules()));

        return v1;
    }
}
