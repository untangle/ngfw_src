/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.util.LinkedList;
import java.util.List;
import java.io.Serializable;

import com.untangle.uvm.generic.RuleActionGeneric;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This in the implementation of a Bypass Rule
 *
 * A rule is basically a collection of BypassRuleConditions (conditions)
 * and what to do if the conditions match (block, log, etc)
 */
@SuppressWarnings("serial")
public class BypassRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

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

    /**
     * Transforms a list of BypassRule objects into their generic RuleGeneric representation.
     * Used for get api calls
     * @param bypassRuleList List<BypassRule>
     * @return LinkedList<RuleGeneric>
     */
    public static LinkedList<RuleGeneric> transformBypassRulesToGeneric(List<BypassRule> bypassRuleList) {
        LinkedList<RuleGeneric> bypassRulesGenList = new LinkedList<>();
        for (BypassRule rule : bypassRuleList) {
            RuleGeneric bypassRulesGen = BypassRule.getBypassRuleGen(rule);
            bypassRulesGenList.add(bypassRulesGen);
        }
        return bypassRulesGenList;
    }

    /**
     * Transforms a BypassRule object into its generic RuleGeneric representation.
     * @param rule BypassRule
     * @return RuleGeneric
     */
    private static RuleGeneric getBypassRuleGen(BypassRule rule) {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(rule.getEnabled());
        String ruleId = rule.getRuleId() != null ? String.valueOf(rule.getRuleId()) : null;

        // Transform Action
        RuleActionGeneric ruleActionGen = new RuleActionGeneric();
        ruleActionGen.setType(Boolean.TRUE.equals(rule.getBypass()) ? RuleActionGeneric.Type.BYPASS : RuleActionGeneric.Type.PROCESS);

        // Transform Conditions
        LinkedList<RuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        for (BypassRuleCondition ruleCondition : rule.getConditions()) {
            String op = ruleCondition.getInvert() ? Constants.IS_NOT_EQUALS_TO : Constants.IS_EQUALS_TO;
            RuleConditionGeneric ruleConditionGen = new RuleConditionGeneric(op, ruleCondition.getConditionType(), ruleCondition.getValue());
            ruleConditionGenList.add(ruleConditionGen);
        }

        // Create Generic Rule
        RuleGeneric bypassRulesGen = new RuleGeneric(enabled, rule.getDescription(), ruleId);
        bypassRulesGen.setAction(ruleActionGen);
        bypassRulesGen.setConditions(ruleConditionGenList);
        return bypassRulesGen;
    }
}

