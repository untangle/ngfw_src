/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention;

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
    private Integer iptablesNfqNumber = 2930;
    private Integer iptablesMaxScanSize = 1024;
    private String iptablesProcessing = "pre";
    private String blockAction = "reject";
    private JSONObject suricataSettings = new JSONObject();

    public IntrusionPreventionSettings() { }

    public IntrusionPreventionSettings(List<IntrusionPreventionRule> rules, List<IntrusionPreventionSignature> signatures, List<IntrusionPreventionVariable> variables, Integer iptablesNfqNumber, Integer iptablesMaxScanSize, String iptablesProcessing)
    {
        this.rules = rules;
        this.signatures = signatures;
        this.variables = variables;
        this.iptablesNfqNumber = iptablesNfqNumber;
        this.iptablesMaxScanSize = iptablesMaxScanSize;
        this.iptablesProcessing = iptablesProcessing;
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
