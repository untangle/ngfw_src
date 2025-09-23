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
 * This in the implementation of a Qos Rule
 *
 * A rule is basically a collection of QosRuleConditions (conditions)
 * and what to do if the conditions match (block, log, etc)
 */
@SuppressWarnings("serial")
public class QosRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<QosRuleCondition> conditions;

    private Integer ruleId;
    private Boolean enabled;
    private String description;

    private int priority;
    
    public QosRule() { }

    public QosRule(boolean enabled, String description, List<QosRuleCondition> conditions, int priority)
    {
        this.setConditions(conditions);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setPriority(priority);
    }

    public List<QosRuleCondition> getConditions() { return this.conditions; }
    public void setConditions( List<QosRuleCondition> conditions ) { this.conditions = conditions; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public int getPriority() { return priority; }
    public void setPriority( int newValue ) { this.priority = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of QosRule objects into their generic RuleGeneric representation.
     * Used for get api calls
     * @param qosRulesList List<QosRule>
     * @return LinkedList<RuleGeneric>
     */
    public static LinkedList<RuleGeneric> transformQoSRulesToGeneric(List<QosRule> qosRulesList) {
        LinkedList<RuleGeneric> qosRulesGenList = new LinkedList<>();
        for (QosRule rule : qosRulesList) {
            RuleGeneric qosRuleGen = getQosRuleGeneric(rule);
            qosRulesGenList.add(qosRuleGen);
        }
        return qosRulesGenList;
    }

    /**
     * Transforms a QosRule object into its generic RuleGeneric representation.
     * @param ruleLegacy QosRule
     * @return RuleGeneric
     */
    private static RuleGeneric getQosRuleGeneric(QosRule ruleLegacy) {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(ruleLegacy.getEnabled());
        String ruleId = ruleLegacy.getRuleId() != null ? String.valueOf(ruleLegacy.getRuleId()) : null;

        // Transform Action
        RuleActionGeneric ruleActionGen = new RuleActionGeneric();
        ruleActionGen.setType(RuleActionGeneric.Type.PRIORITY);
        ruleActionGen.setPriority(ruleLegacy.getPriority());

        // Transform Conditions
        LinkedList<RuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        for (QosRuleCondition ruleCondition : ruleLegacy.getConditions()) {
            String op = ruleCondition.getInvert() ? Constants.IS_NOT_EQUALS_TO : Constants.IS_EQUALS_TO;
            RuleConditionGeneric ruleConditionGen = new RuleConditionGeneric(op, ruleCondition.getConditionType(), ruleCondition.getValue());
            ruleConditionGenList.add(ruleConditionGen);
        }

        // Create Generic Rule
        RuleGeneric qosRuleGen = new RuleGeneric(enabled, ruleLegacy.getDescription(), ruleId);
        qosRuleGen.setAction(ruleActionGen);
        qosRuleGen.setConditions(ruleConditionGenList);
        return qosRuleGen;
    }
}

