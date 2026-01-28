/**
 * $Id$
 */
package com.untangle.app.policy_manager;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.generic.RuleActionGeneric;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.StringUtil;

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

      /**
     * Transforms a list of Policy RuleGeneric objects into their v1 PolicyRule representation.
     * Used for set api calls
     *
     * @param policyRulesGen LinkedList<RuleGeneric>
     * @param legacyRules List<PolicyRule>
     * @return List<PolicyRule>
     */
    public static List<PolicyRule> transformGenericToLegacyPolicyRules(LinkedList<RuleGeneric> policyRulesGen, List<PolicyRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();
        // CLEANUP: Remove deleted rules first
        RuleGeneric.deleteOrphanRules(
                policyRulesGen,
                legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );
        // Build a map for quick lookup by ruleId
        Map<Integer, PolicyRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(PolicyRule::getRuleId, Function.identity()));
        LinkedList<PolicyRule> policyRules = new LinkedList<>();
        for (RuleGeneric ruleGeneric : policyRulesGen) {
            PolicyRule policyRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));
            policyRule = PolicyRule.transformPolicyRule(ruleGeneric, policyRule);
            policyRules.add(policyRule);
        }
        return policyRules;
    }
    
    /**
     * Transforms a Policy Rule Generic object into v1 PolicyRule representation.
     * @param ruleGeneric RuleGeneric
     * @param policyRule PolicyRule
     * @return PolicyRule
     */
    private static PolicyRule transformPolicyRule(RuleGeneric ruleGeneric, PolicyRule policyRule) {
        if (policyRule == null)
            policyRule = new PolicyRule();
        // Transform enabled, ruleId, description
        policyRule.setEnabled(ruleGeneric.isEnabled());
        policyRule.setDescription(ruleGeneric.getDescription());
        // For new rules added UI send uuid string as ruleId. Here its set to -1
        policyRule.setRuleId(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), -1));
        // Transform Action
        if (ruleGeneric.getAction() != null)
            policyRule.setTargetPolicy(ruleGeneric.getAction().getTargetPolicy());
        // Transform Conditions
        List<PolicyRuleCondition> ruleConditions = new LinkedList<>();
        for (RuleConditionGeneric ruleConditionGen : ruleGeneric.getConditions()) {
            PolicyRuleCondition policyRuleCondition = new PolicyRuleCondition();
            policyRuleCondition.setInvert(ruleConditionGen.getOp().equals(Constants.IS_NOT_EQUALS_TO));
            policyRuleCondition.setConditionType(ruleConditionGen.getType());
            policyRuleCondition.setValue(ruleConditionGen.getValue());
            ruleConditions.add(policyRuleCondition);
        }
        policyRule.setConditions(ruleConditions);
        return policyRule;
    }
}

