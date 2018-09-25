/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention;

import org.json.JSONObject;
import org.json.JSONString;
import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

/**
 * Settings for the Active Directory (really a bunch of LDAP settings).
 */
@SuppressWarnings("serial")
public class IntrusionPreventionSettings implements Serializable, JSONString
{
    private Integer version = 3;
    private List<IntrusionPreventionRule> rules = new LinkedList<>();
    private List<String> signatures = new LinkedList<>();
    private Map<String, String> variables = new HashMap<String, String>();
    private Integer iptablesNfqNumber = 2930;
    private Integer iptablesMaxScanSize = 1024;
    private JSONObject suricataSettings = new JSONObject();

    public IntrusionPreventionSettings() { }

    public IntrusionPreventionSettings(List<IntrusionPreventionRule> rules, List<String> signatures, Map<String, String> variables, Integer iptablesNfqNumber, Integer iptablesMaxScanSize)
    {
        this.rules = rules;
        this.signatures = signatures;
        this.variables = variables;
        this.iptablesNfqNumber = iptablesNfqNumber;
        this.iptablesMaxScanSize = iptablesMaxScanSize;
    }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public List<IntrusionPreventionRule> getRules() { return rules; }
    public void setRules(List<IntrusionPreventionRule> signatures) { this.rules = rules; }

    public List<String> getSignatures() { return signatures; }
    public void setSignatures(List<String> signatures) { this.signatures = signatures; }

    public Map<String,String> getVariables() { return variables; }
    public void setVariables(Map<String, String> variables) { this.variables = variables; }

    public JSONObject getSuricataSettings() { return suricataSettings; }
    public void setSuricataSettings(JSONObject suricataSettings) { this.suricataSettings = suricataSettings; }

    public Integer getIptablesNfqNumber() { return iptablesNfqNumber; }
    public void setIptablesNfqNumber(Integer iptableNfqNumber) { this.iptablesNfqNumber = iptablesNfqNumber; }

    public Integer getIptablesMaxScanSize() { return iptablesMaxScanSize; }
    public void setIptablesMaxScanSize(Integer iptablesMaxScanSize) { this.iptablesMaxScanSize = iptablesMaxScanSize; }

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

    /**
     * Intrusion prevention rule
     */
    private class IntrusionPreventionRule implements Serializable, JSONString
    {
        private String action = "default";
        private List<IntrusionPreventionRuleCondition> conditions = new LinkedList<>();
        private String description = "";
        private Boolean enabled = false;
        private Integer id = -1;

        public IntrusionPreventionRule() { }

        public IntrusionPreventionRule(String action, List<IntrusionPreventionRuleCondition> conditions, String description, Boolean enabled, Integer id)
        {
            this.action = action;
            this.conditions = conditions;
            this.description = description;
            this.enabled = enabled;
            this.id = id;
        }

        public String getAction() { return action; }
        public void setVersion(String action) { this.action = action; }

        public List<IntrusionPreventionRuleCondition> getConditions() { return conditions; }
        public void setConditions(List<IntrusionPreventionRuleCondition> signatures) { this.conditions = conditions; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String toJSONString()
        {
            JSONObject jO = new JSONObject(this);
            return jO.toString();
        }
    }

    /**
     * Rule condition
     */
    private class IntrusionPreventionRuleCondition implements Serializable, JSONString
    {
        private String comparator = "=";
        private String type = "";
        private String value = "";

        public IntrusionPreventionRuleCondition() { }

        public IntrusionPreventionRuleCondition(String comparator, String type, String value)
        {
            this.comparator = comparator;
            this.type = type;
            this.value = value;
        }

        public String getComparator() { return comparator; }
        public void setComparator(String description) { this.comparator = comparator; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String toJSONString()
        {
            JSONObject jO = new JSONObject(this);
            return jO.toString();
        }
    } 
}
