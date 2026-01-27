/**
 * $Id$
 */
package com.untangle.app.policy_manager;

import java.util.LinkedList;
import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.generic.RuleActionGeneric;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This in the implementation of a Policy Rule
 *
 * A rule is basically a collection of PolicyRuleConditions (matchers)
 * and what to do if the matchers match (targetPolicy)
 */
@SuppressWarnings("serial")
public class PolicyRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<PolicyRuleCondition> conditions;

    private Integer ruleId;
    private Boolean enabled;
    private String description;
    private Integer targetPolicy;
    
    public PolicyRule()
    {
    }

    public PolicyRule(boolean enabled, List<PolicyRuleCondition> conditions, Integer targetPolicy, String description)
    {
        this.setConditions(conditions);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setTargetPolicy(targetPolicy);
        this.setDescription(description);
    }
    
    public List<PolicyRuleCondition> getConditions() { return this.conditions; }
    public void setConditions( List<PolicyRuleCondition> conditions ) { this.conditions = conditions; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public Integer getTargetPolicy() { return targetPolicy; }
    public void setTargetPolicy( Integer targetPolicy ) { this.targetPolicy = targetPolicy; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
    public boolean isMatch( short protocol,
                            int srcIntf, int dstIntf,
                            InetAddress srcAddress, InetAddress dstAddress,
                            int srcPort, int dstPort)
    {
        if (!getEnabled())
            return false;

        /**
         * If no conditions return true
         */
        if (this.conditions == null) {
            logger.warn("Null conditions - assuming true");
            return true;
        }

        /**
         * IF any matcher doesn't match - return false
         */
        for ( PolicyRuleCondition matcher : conditions ) {
            if (!matcher.matches( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort, null))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
    
    /**
     * Transforms a list of PolicyRule objects into their generic RuleGeneric representation.
     * Used for get api calls
     *
     * @param policyRuleList List<FilterRule>
     * @return LinkedList<RuleGeneric>
     */
    public static LinkedList<RuleGeneric> transformPolicyRulesToGeneric(List<PolicyRule> policyRuleList) {
        LinkedList<RuleGeneric> policyRulesGenList = new LinkedList<>();
        for (PolicyRule rule : policyRuleList) {
            RuleGeneric policyRulesGen = PolicyRule.getPolicyRuleGeneric(rule);
            policyRulesGenList.add(policyRulesGen);
        }
        return policyRulesGenList;
    }

    /**
     * Transforms a PolicyRule object into its generic RuleGeneric representation.
     * @param ruleLegacy PolicyRule
     * @return RuleGeneric
     */
    private static RuleGeneric getPolicyRuleGeneric(PolicyRule ruleLegacy) {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(ruleLegacy.getEnabled());
        String ruleId = ruleLegacy.getRuleId() != null ? String.valueOf(ruleLegacy.getRuleId()) : null;

        // Transform Action
        RuleActionGeneric ruleActionGen = new RuleActionGeneric();
        ruleActionGen.setType(RuleActionGeneric.Type.TARGET_POLICY);
        ruleActionGen.setTargetPolicy(ruleLegacy.getTargetPolicy());

        // Transform Conditions
        LinkedList<RuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        for (PolicyRuleCondition ruleCondition : ruleLegacy.getConditions()) {
            String op = ruleCondition.getInvert() ? Constants.IS_NOT_EQUALS_TO : Constants.IS_EQUALS_TO;
            RuleConditionGeneric ruleConditionGen = new RuleConditionGeneric(op, ruleCondition.getConditionType(), ruleCondition.getValue());
            ruleConditionGenList.add(ruleConditionGen);
        }

        // Create Generic Rule
        RuleGeneric policyRuleGen = new RuleGeneric(enabled, ruleLegacy.getDescription(), ruleId);
        policyRuleGen.setAction(ruleActionGen);
        policyRuleGen.setConditions(ruleConditionGenList);
        return policyRuleGen;
    }
}

