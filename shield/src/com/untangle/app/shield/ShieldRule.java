/**
 * $Id$
 */
package com.untangle.app.shield;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.Serializable;
import java.net.InetAddress;

import com.untangle.uvm.generic.RuleActionGeneric;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.StringUtil;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This in the implementation of a Shield Rule
 *
 * A rule is basically a collection of ShieldRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class ShieldRule implements JSONString, Serializable
{
    private static final Logger logger = LogManager.getLogger(ShieldRule.class);

    public enum ShieldRuleAction
    {
        SCAN, PASS
    }

    private List<ShieldRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private String description;
    private ShieldRuleAction action = ShieldRuleAction.SCAN;
    
    public ShieldRule()
    {
    }

    public ShieldRule(boolean enabled, List<ShieldRuleCondition> matchers, boolean flag, boolean block, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setDescription(description);
    }
    
    public List<ShieldRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<ShieldRuleCondition> newValue ) { this.matchers = newValue; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public ShieldRuleAction getAction() { return action; }
    public void setAction( ShieldRuleAction newValue ) { this.action = newValue; }
    
    /**
     * DEPRECATED in v13 - 13.0 conversion
     * Multiplier has been remove in v13
     * It is kept here for now so that JSON serialization still works and the settings can be converted
     * This can be removed after v13
     */
    private int multiplier = 1;
    public int getMultiplier() { return multiplier; }
    public void setMultiplier( int newValue ) { this.multiplier = newValue; }

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
         * If no matchers return true
         */
        if (this.matchers == null) {
            logger.warn("Null matchers - assuming true");
            return true;
        }

        /**
         * IF any matcher doesn't match - return false
         */
        for ( ShieldRuleCondition matcher : matchers ) {
            if (!matcher.matches(protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort ))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }

    /**
     * Transforms a list of ShieldRule objects into their generic RuleGeneric representation.
     * Used for get api calls
     * @param shieldRulesList LinkedList<ShieldRule>
     * @return LinkedList<RuleGeneric>
     */
    public static LinkedList<RuleGeneric> transformShieldRulesToGeneric(LinkedList<ShieldRule> shieldRulesList) {
        LinkedList<RuleGeneric> shieldRulesGenList = new LinkedList<>();
        for (ShieldRule rule : shieldRulesList) {
            RuleGeneric shieldRuleGen = getShieldRuleGeneric(rule);
            shieldRulesGenList.add(shieldRuleGen);
        }
        return shieldRulesGenList;
    }

    /**
     * Transforms a ShieldRule object into its generic RuleGeneric representation.
     * @param ruleLegacy ShieldRule
     * @return RuleGeneric
     */
    private static RuleGeneric getShieldRuleGeneric(ShieldRule ruleLegacy) {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(ruleLegacy.getEnabled());
        String ruleId = ruleLegacy.getRuleId() != null ? String.valueOf(ruleLegacy.getRuleId()) : null;

        // Transform Action
        RuleActionGeneric ruleActionGen = new RuleActionGeneric();
        ruleActionGen.setType(ruleLegacy.getAction() == ShieldRuleAction.PASS ? RuleActionGeneric.Type.PASS : RuleActionGeneric.Type.SCAN);

        // Transform Conditions
        LinkedList<RuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        for (ShieldRuleCondition ruleCondition : ruleLegacy.getConditions()) {
            String op = ruleCondition.getInvert() ? Constants.IS_NOT_EQUALS_TO : Constants.IS_EQUALS_TO;
            RuleConditionGeneric ruleConditionGen = new RuleConditionGeneric(op, ruleCondition.getConditionType(), ruleCondition.getValue());
            ruleConditionGenList.add(ruleConditionGen);
        }

        // Create Generic Rule
        RuleGeneric shieldRuleGen = new RuleGeneric(enabled, ruleLegacy.getDescription(), ruleId);
        shieldRuleGen.setAction(ruleActionGen);
        shieldRuleGen.setConditions(ruleConditionGenList);
        return shieldRuleGen;
    }

    /**
     * Transforms a list of Shield RuleGeneric objects into their v1 ShieldRule representation.
     * Used for set api calls
     * @param shieldRulesGen LinkedList<RuleGeneric>
     * @param legacyRules List<ShieldRule>
     * @return List<ShieldRule
     */
    public static LinkedList<ShieldRule> transformGenericToShieldRules(LinkedList<RuleGeneric> shieldRulesGen, List<ShieldRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();

        // CLEANUP: Remove deleted rules first
        RuleGeneric.deleteOrphanRules(
                shieldRulesGen,
                legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );

        // Build a map for quick lookup by ruleId
        Map<Integer, ShieldRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(ShieldRule::getRuleId, Function.identity()));

        LinkedList<ShieldRule> shieldRules = new LinkedList<>();
        for (RuleGeneric ruleGeneric : shieldRulesGen) {
            ShieldRule shieldRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));
            shieldRule = ShieldRule.transformShieldRule(ruleGeneric, shieldRule);
            shieldRules.add(shieldRule);
        }
        return shieldRules;
    }

    /**
     * Transforms a Shield Rule Generic object into v1 ShieldRule representation.
     * @param ruleGeneric RuleGeneric
     * @param shieldRule ShieldRule
     * @return ShieldRule
     */
    private static ShieldRule transformShieldRule(RuleGeneric ruleGeneric, ShieldRule shieldRule) {
        if (shieldRule == null)
            shieldRule = new ShieldRule();

        // Transform enabled, ruleId, description
        shieldRule.setEnabled(ruleGeneric.isEnabled());
        shieldRule.setDescription(ruleGeneric.getDescription());
        // For new rules added UI send uuid string as ruleId. Here its set to -1
        shieldRule.setRuleId(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), -1));

        // Transform Action
        if (ruleGeneric.getAction() != null)
            shieldRule.setAction(ruleGeneric.getAction().getType() == RuleActionGeneric.Type.SCAN
                    ? ShieldRule.ShieldRuleAction.SCAN
                    : ShieldRule.ShieldRuleAction.PASS);

        // Transform Conditions
        List<ShieldRuleCondition> ruleConditions = new LinkedList<>();
        for (RuleConditionGeneric ruleConditionGen : ruleGeneric.getConditions()) {
            ShieldRuleCondition shieldRuleCondition = new ShieldRuleCondition();

            shieldRuleCondition.setInvert(ruleConditionGen.getOp().equals(Constants.IS_NOT_EQUALS_TO));
            shieldRuleCondition.setConditionType(ruleConditionGen.getType());
            shieldRuleCondition.setValue(ruleConditionGen.getValue());

            ruleConditions.add(shieldRuleCondition);
        }
        shieldRule.setConditions(ruleConditions);
        return shieldRule;
    }

}

