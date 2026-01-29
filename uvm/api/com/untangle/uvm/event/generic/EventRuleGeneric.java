/**
 * $Id$
 */
package com.untangle.uvm.event.generic;

import com.untangle.uvm.event.AlertRule;
import com.untangle.uvm.event.EventRuleCondition;
import com.untangle.uvm.event.TriggerRule;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.StringUtil;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This in the Generic Rule Class for Event Rules
 * Used for vue model transformations
 */
@SuppressWarnings("serial")
public class EventRuleGeneric implements JSONString, Serializable {

    public EventRuleGeneric() {}

    public EventRuleGeneric(boolean enabled, String description, String ruleId) {
        this.enabled = enabled;
        this.description = description;
        this.ruleId = ruleId;
    }

    // Common To All Rules
    private boolean enabled;
    private String description;
    private String ruleId;
    private String className;
    private EventRuleActionGeneric action;
    private LinkedList<EventRuleConditionGeneric> conditions;

    private Boolean log = false;
    private Boolean thresholdEnabled = false;
    private Double  thresholdLimit;
    private Integer thresholdTimeframeSec;
    private String  thresholdGroupingField;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public EventRuleActionGeneric getAction() { return action; }
    public void setAction(EventRuleActionGeneric action) { this.action = action; }
    public LinkedList<EventRuleConditionGeneric> getConditions() { return conditions; }
    public void setConditions(LinkedList<EventRuleConditionGeneric> conditions) { this.conditions = conditions; }

    public Boolean getLog() { return log; }
    public void setLog(Boolean log) { this.log = log; }
    public Boolean getThresholdEnabled() { return thresholdEnabled; }
    public void setThresholdEnabled(Boolean thresholdEnabled) { this.thresholdEnabled = thresholdEnabled; }
    public Double getThresholdLimit() { return thresholdLimit; }
    public void setThresholdLimit(Double thresholdLimit) { this.thresholdLimit = thresholdLimit; }
    public Integer getThresholdTimeframeSec() { return thresholdTimeframeSec; }
    public void setThresholdTimeframeSec(Integer thresholdTimeframeSec) { this.thresholdTimeframeSec = thresholdTimeframeSec; }
    public String getThresholdGroupingField() { return thresholdGroupingField; }
    public void setThresholdGroupingField(String thresholdGroupingField) { this.thresholdGroupingField = thresholdGroupingField; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of Alert EventRuleGeneric objects into their v1 AlertRule representation.
     * Used for set api calls
     * @param alertRulesGen LinkedList<EventRuleGeneric>
     * @param legacyRules LinkedList<AlertRule>
     * @return List<AlertRule>
     */
    public static LinkedList<AlertRule> transformGenericToLegacyAlertRules(LinkedList<EventRuleGeneric> alertRulesGen, LinkedList<AlertRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();

        // CLEANUP: Remove deleted rules first
        RuleGeneric.deleteOrphanRules(
                alertRulesGen,
                legacyRules,
                EventRuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );

        // Build a map for quick lookup by ruleId
        Map<Integer, AlertRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(AlertRule::getRuleId, Function.identity()));

        LinkedList<AlertRule> alertRules = new LinkedList<>();
        for (EventRuleGeneric ruleGeneric : alertRulesGen) {
            AlertRule alertRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));
            alertRule = EventRuleGeneric.transformAlertRule(ruleGeneric, alertRule);
            alertRules.add(alertRule);
        }
        return alertRules;
    }

    /**
     * Transforms a Alert Rule Generic object into v1 AlertRule representation.
     * @param ruleGeneric EventRuleGeneric
     * @param alertRule AlertRule
     * @return AlertRule
     */
    private static AlertRule transformAlertRule(EventRuleGeneric ruleGeneric, AlertRule alertRule) {
        if (alertRule == null)
            alertRule = new AlertRule();

        // Transform enabled, ruleId, description
        alertRule.setEnabled(ruleGeneric.isEnabled());
        alertRule.setDescription(ruleGeneric.getDescription());
        // For new rules added UI send uuid string as ruleId. Here its set to -1
        alertRule.setRuleId(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), -1));

        // Transform Action
        alertRule.setLog(ruleGeneric.getLog());
        if (ruleGeneric.getAction() != null) {
            alertRule.setEmail(ruleGeneric.getAction().getType() == EventRuleActionGeneric.Type.EMAIL);
            alertRule.setEmailLimitFrequency(ruleGeneric.getAction().getEmailLimitFrequency());
            alertRule.setEmailLimitFrequencyMinutes(ruleGeneric.getAction().getEmailLimitFrequencyMinutes());
        }

        // Transform Conditions
        List<EventRuleCondition> ruleConditions = new LinkedList<>();
        EventRuleCondition classCondition = new EventRuleCondition(Constants.CLASS, Constants.EQUALS_TO, ruleGeneric.getClassName());
        ruleConditions.add(classCondition);
        for (EventRuleConditionGeneric ruleConditionGen : ruleGeneric.getConditions()) {
            EventRuleCondition eventRuleCondition = new EventRuleCondition();

            eventRuleCondition.setComparator(ruleConditionGen.getOp());
            eventRuleCondition.setField(ruleConditionGen.getType());
            eventRuleCondition.setFieldValue(ruleConditionGen.getValue());

            ruleConditions.add(eventRuleCondition);
        }
        alertRule.setConditions(ruleConditions);
        alertRule.setThresholdEnabled(ruleGeneric.getThresholdEnabled());
        alertRule.setThresholdLimit(ruleGeneric.getThresholdLimit());
        alertRule.setThresholdTimeframeSec(ruleGeneric.getThresholdTimeframeSec());
        alertRule.setThresholdGroupingField(ruleGeneric.getThresholdGroupingField());
        return alertRule;
    }

    /**
     * Transforms a list of Trigger EventRuleGeneric objects into their v1 TriggerRule representation.
     * Used for set api calls
     * @param triggerRulesGen LinkedList<EventRuleGeneric>
     * @param legacyRules LinkedList<TriggerRule>
     * @return List<TriggerRule>
     */
    public static LinkedList<TriggerRule> transformGenericToLegacyTriggerRules(LinkedList<EventRuleGeneric> triggerRulesGen, LinkedList<TriggerRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();

        // CLEANUP: Remove deleted rules first
        RuleGeneric.deleteOrphanRules(
                triggerRulesGen,
                legacyRules,
                EventRuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );

        // Build a map for quick lookup by ruleId
        Map<Integer, TriggerRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(TriggerRule::getRuleId, Function.identity()));

        LinkedList<TriggerRule> triggerRules = new LinkedList<>();
        for (EventRuleGeneric ruleGeneric : triggerRulesGen) {
            TriggerRule triggerRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));
            triggerRule = EventRuleGeneric.transformTriggerRule(ruleGeneric, triggerRule);
            triggerRules.add(triggerRule);
        }
        return triggerRules;
    }

    /**
     * Transforms a Trigger Rule Generic object into v1 TriggerRule representation.
     * @param ruleGeneric EventRuleGeneric
     * @param triggerRule TriggerRule
     * @return TriggerRule
     */
    private static TriggerRule transformTriggerRule(EventRuleGeneric ruleGeneric, TriggerRule triggerRule) {
        if (triggerRule == null)
            triggerRule = new TriggerRule();

        // Transform enabled, ruleId, description
        triggerRule.setEnabled(ruleGeneric.isEnabled());
        triggerRule.setDescription(ruleGeneric.getDescription());
        // For new rules added UI send uuid string as ruleId. Here its set to -1
        triggerRule.setRuleId(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), -1));

        // Transform Action
        triggerRule.setLog(ruleGeneric.getLog());
        if (ruleGeneric.getAction() != null) {
            triggerRule.setAction(TriggerRule.TriggerAction.valueOf(ruleGeneric.getAction().getType().name()));
            triggerRule.setTagTarget(ruleGeneric.getAction().getTagTarget());
            triggerRule.setTagName(ruleGeneric.getAction().getTagName());
            triggerRule.setTagLifetimeSec(ruleGeneric.getAction().getTagLifetimeSec());
        }

        // Transform Conditions
        List<EventRuleCondition> ruleConditions = new LinkedList<>();
        EventRuleCondition classCondition = new EventRuleCondition(Constants.CLASS, Constants.EQUALS_TO, ruleGeneric.getClassName());
        ruleConditions.add(classCondition);
        for (EventRuleConditionGeneric ruleConditionGen : ruleGeneric.getConditions()) {
            EventRuleCondition eventRuleCondition = new EventRuleCondition();

            eventRuleCondition.setComparator(ruleConditionGen.getOp());
            eventRuleCondition.setField(ruleConditionGen.getType());
            eventRuleCondition.setFieldValue(ruleConditionGen.getValue());

            ruleConditions.add(eventRuleCondition);
        }
        triggerRule.setConditions(ruleConditions);
        triggerRule.setThresholdEnabled(ruleGeneric.getThresholdEnabled());
        triggerRule.setThresholdLimit(ruleGeneric.getThresholdLimit());
        triggerRule.setThresholdTimeframeSec(ruleGeneric.getThresholdTimeframeSec());
        triggerRule.setThresholdGroupingField(ruleGeneric.getThresholdGroupingField());
        return triggerRule;
    }
}
