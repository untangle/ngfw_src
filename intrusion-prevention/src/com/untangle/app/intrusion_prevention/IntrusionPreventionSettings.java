/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention;

import com.untangle.uvm.network.BypassRule;

import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;

/**
 * Settings for Intrusion Prevenion.
 */
@SuppressWarnings("serial")
public class IntrusionPreventionSettings implements Serializable, JSONString
{
    private Integer version = 3;
    private String defaultsMd5sum = "";
    private String classificationMd5sum = "";
    private String variablesMd5sum = "";
    private List<IntrusionPreventionRule> rules = new LinkedList<>();
    private List<IntrusionPreventionSignature> signatures = new LinkedList<>();
    private List<IntrusionPreventionVariable> variables = new LinkedList<>();
    private List<BypassRule> bypassRules = new LinkedList<>();
    private Integer iptablesNfqNumber = 2930;
    private Integer iptablesMaxScanSize = 1024;
    private String iptablesProcessing = "pre";
    private String blockAction = "reject";
    private JSONObject suricataSettings = new JSONObject();
    private String updateSignatureFrequency = "Daily";
    private List<IntrusionPreventionDaySchedule> updateSignatureSchedule = new LinkedList<>();
    private IntrusionPreventionDaySchedule updateSignatureWeekly = new IntrusionPreventionDaySchedule();

    public IntrusionPreventionSettings() 
    { 
        //Default is a 7 day schedule with -1 for hours/minute
        updateSignatureSchedule.add(new IntrusionPreventionDaySchedule("Sunday"));
        updateSignatureSchedule.add(new IntrusionPreventionDaySchedule("Monday"));
        updateSignatureSchedule.add(new IntrusionPreventionDaySchedule("Tuesday"));
        updateSignatureSchedule.add(new IntrusionPreventionDaySchedule("Wednesday"));
        updateSignatureSchedule.add(new IntrusionPreventionDaySchedule("Thursday"));
        updateSignatureSchedule.add(new IntrusionPreventionDaySchedule("Friday"));
        updateSignatureSchedule.add(new IntrusionPreventionDaySchedule("Saturday"));
    }

    public IntrusionPreventionSettings(List<IntrusionPreventionRule> rules, List<IntrusionPreventionSignature> signatures, List<IntrusionPreventionVariable> variables, Integer iptablesNfqNumber, Integer iptablesMaxScanSize, String iptablesProcessing, List<IntrusionPreventionDaySchedule> updateSignatureSchedule, String updateSignatureFrequency, IntrusionPreventionDaySchedule updateSignatureWeekly)
    {
        this.rules = rules;
        this.signatures = signatures;
        this.variables = variables;
        this.iptablesNfqNumber = iptablesNfqNumber;
        this.iptablesMaxScanSize = iptablesMaxScanSize;
        this.iptablesProcessing = iptablesProcessing;
        this.updateSignatureSchedule = updateSignatureSchedule;
        this.updateSignatureFrequency = updateSignatureFrequency;
        this.updateSignatureWeekly = updateSignatureWeekly;
    }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getDefaultsMd5sum() { return defaultsMd5sum; }
    public void setDefaultsMd5sum(String defaultsMd5sum) { this.defaultsMd5sum = defaultsMd5sum; }

    public String getClassificationMd5sum() { return classificationMd5sum; }
    public void setClassificationMd5sum(String classificationMd5sum) { this.classificationMd5sum = classificationMd5sum; }

    public String getVariablesMd5sum() { return variablesMd5sum; }
    public void setVariablesMd5sum(String variablesMd5sum) { this.variablesMd5sum = variablesMd5sum; }

    public List<IntrusionPreventionRule> getRules() { return rules; }
    public void setRules(List<IntrusionPreventionRule> rules) { this.rules = rules; }

    public List<IntrusionPreventionSignature> getSignatures() { return signatures; }
    public void setSignatures(List<IntrusionPreventionSignature> signatures) { this.signatures = signatures; }

    public List<IntrusionPreventionVariable> getVariables() { return variables; }
    public void setVariables(List<IntrusionPreventionVariable> variables) { this.variables = variables; }

    public List<BypassRule> getBypassRules() { return this.bypassRules; }
    public void setBypassRules( List<BypassRule> newValue ) { this.bypassRules = newValue; }

    public JSONObject getSuricataSettings() { return suricataSettings; }
    public void setSuricataSettings(JSONObject suricataSettings) { this.suricataSettings = suricataSettings; }

    public Integer getIptablesNfqNumber() { return iptablesNfqNumber; }
    public void setIptablesNfqNumber(Integer iptableNfqNumber) { this.iptablesNfqNumber = iptablesNfqNumber; }

    public Integer getIptablesMaxScanSize() { return iptablesMaxScanSize; }
    public void setIptablesMaxScanSize(Integer iptablesMaxScanSize) { this.iptablesMaxScanSize = iptablesMaxScanSize; }

    public String getIptablesProcessing() { return iptablesProcessing; }
    public void setIptablesProcessing(String iptablesProcessing) { this.iptablesProcessing = iptablesProcessing; }

    public String getBlockAction() { return blockAction; }
    public void setBlockAction(String blockAction) { this.blockAction = blockAction; }

    public List<IntrusionPreventionDaySchedule> getUpdateSignatureSchedule() { return this.updateSignatureSchedule; }
    public void setUpdateSignatureSchedule(List<IntrusionPreventionDaySchedule> updateSignatureSchedule) { this.updateSignatureSchedule = updateSignatureSchedule; }

    public String getUpdateSignatureFrequency() { return this.updateSignatureFrequency; }
    public void setUpdateSignatureFrequency(String updateSignatureFrequency) { this.updateSignatureFrequency = updateSignatureFrequency; }

    public IntrusionPreventionDaySchedule getUpdateSignatureWeekly() { return this.updateSignatureWeekly; }
    public void setUpdateSignatureWeekly(IntrusionPreventionDaySchedule updateSignatureWeekly) { this.updateSignatureWeekly = updateSignatureWeekly; }

    /**
     * Returns settings as a JSON string.
     *
     * @return
     *      Server settings in JSON form.
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
