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
 * This in the implementation of a Filter Rule
 *
 * A rule is basically a collection of FilterRuleConditions (conditions)
 * and what to do if the conditions match (block, log, etc)
 */
@SuppressWarnings("serial")
public class FilterRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<FilterRuleCondition> conditions;

    private Integer ruleId;
    private boolean enabled = true;
    private boolean ipv6Enabled = true;
    private boolean blocked = false;
    private Boolean readOnly = null;
    private String description;
    
    public FilterRule() { }

    public FilterRule(boolean enabled, boolean ipv6Enabled, List<FilterRuleCondition> conditions, boolean blocked, String description)
    {
        this.setConditions(conditions);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setIpv6Enabled(Boolean.valueOf(ipv6Enabled));
        this.setBlocked(blocked);
        this.setDescription(description);
    }

    public List<FilterRuleCondition> getConditions() { return this.conditions; }
    public void setConditions( List<FilterRuleCondition> conditions ) { this.conditions = conditions; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public boolean getEnabled() { return enabled; }
    public void setEnabled( boolean enabled ) { this.enabled = enabled; }

    public boolean getIpv6Enabled() { return ipv6Enabled; }
    public void setIpv6Enabled( boolean ipv6Enabled ) { this.ipv6Enabled = ipv6Enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public boolean getBlocked() { return blocked; }
    public void setBlocked( boolean newDestination ) { this.blocked = newDestination; }

    public Boolean getReadOnly() { return this.readOnly; }
    public void setReadOnly( Boolean newValue ) { this.readOnly = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of FilterRule objects into their generic RuleGeneric representation.
     * Used for get api calls
     * @param filterRuleList List<FilterRule>
     * @return LinkedList<RuleGeneric>
     */
    public static LinkedList<RuleGeneric> transformFilterRulesToGeneric(List<FilterRule> filterRuleList) {
        LinkedList<RuleGeneric> filterRulesGenList = new LinkedList<>();
        for (FilterRule rule : filterRuleList) {
            RuleGeneric filterRulesGen = FilterRule.getFilterRuleGeneric(rule);
            filterRulesGenList.add(filterRulesGen);
        }
        return filterRulesGenList;
    }

    /**
     * Transforms a FilterRule object into its generic RuleGeneric representation.
     * @param rule FilterRule
     * @return RuleGeneric
     */
    private static RuleGeneric getFilterRuleGeneric(FilterRule rule) {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(rule.getEnabled());
        String ruleId = rule.getRuleId() != null ? String.valueOf(rule.getRuleId()) : null;

        // Transform Action
        RuleActionGeneric ruleActionGen = new RuleActionGeneric();
        ruleActionGen.setType(rule.getBlocked() ? RuleActionGeneric.Type.REJECT : RuleActionGeneric.Type.ACCEPT);

        // Transform Conditions
        LinkedList<RuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        for (FilterRuleCondition ruleCondition : rule.getConditions()) {
            String op = ruleCondition.getInvert() ? Constants.IS_NOT_EQUALS_TO : Constants.IS_EQUALS_TO;
            RuleConditionGeneric ruleConditionGen = new RuleConditionGeneric(op, ruleCondition.getConditionType(), ruleCondition.getValue());
            ruleConditionGenList.add(ruleConditionGen);
        }

        // Create Generic Rule
        RuleGeneric filterRulesGen = new RuleGeneric(enabled, rule.getDescription(), ruleId);
        filterRulesGen.setIpv6Enabled(rule.getIpv6Enabled());
        filterRulesGen.setReadOnlyRule(rule.getReadOnly());
        filterRulesGen.setAction(ruleActionGen);
        filterRulesGen.setConditions(ruleConditionGenList);
        return filterRulesGen;
    }
}

