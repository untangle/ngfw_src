/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.util.List;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This in the implementation of a Bypass Rule
 *
 * A rule is basically a collection of BypassRuleConditions (conditions)
 * and what to do if the conditions match (block, log, etc)
 */
@SuppressWarnings("serial")
public class BypassRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<BypassRuleCondition> conditions;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean bypass;
    private String description;
    
    public BypassRule() { }

    public BypassRule(boolean enabled, List<BypassRuleCondition> conditions, boolean bypass, String description)
    {
        this.setConditions(conditions);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setBypass(bypass);
        this.setDescription(description);
    }
    
    public List<BypassRuleCondition> getConditions() { return this.conditions; }
    public void setConditions( List<BypassRuleCondition> conditions ) { this.conditions = conditions; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public Boolean getBypass() { return bypass; }
    public void setBypass( Boolean bypass ) { this.bypass = bypass; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

