/**
 * $Id$
 */
package com.untangle.uvm.generic;


import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * This in the Generic Rule Class
 * Used for vue model transformations
 */
@SuppressWarnings("serial")
public class RuleGeneric implements JSONString, Serializable {

    public RuleGeneric() {}

    public RuleGeneric(boolean enabled, String description, String ruleId) {
        this.enabled = enabled;
        this.description = description;
        this.ruleId = ruleId;
    }

    // Common To All Rules
    private boolean enabled;
    private String description;
    private String ruleId;
    private RuleActionGeneric action;
    private LinkedList<RuleConditionGeneric> conditions;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public RuleActionGeneric getAction() { return action; }
    public void setAction(RuleActionGeneric action) { this.action = action; }
    public LinkedList<RuleConditionGeneric> getConditions() { return conditions; }
    public void setConditions(LinkedList<RuleConditionGeneric> conditions) { this.conditions = conditions; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
