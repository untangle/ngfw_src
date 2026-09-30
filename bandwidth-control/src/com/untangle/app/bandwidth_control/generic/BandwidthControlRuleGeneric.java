/**
 * $Id$
 */
package com.untangle.app.bandwidth_control.generic;

import com.untangle.app.bandwidth_control.BandwidthControlRule;
import com.untangle.app.bandwidth_control.BandwidthControlRuleAction;
import com.untangle.app.bandwidth_control.BandwidthControlRuleCondition;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.util.Constants;

import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic (V2) rule for Bandwidth Control, used for Vue UI transformations.
 */
@SuppressWarnings("serial")
public class BandwidthControlRuleGeneric implements JSONString, Serializable {

    private boolean enabled;
    private String description;
    private String ruleId;
    private BandwidthControlActionGeneric action;
    private LinkedList<RuleConditionGeneric> conditions;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public BandwidthControlActionGeneric getAction() { return action; }
    public void setAction(BandwidthControlActionGeneric action) { this.action = action; }

    public LinkedList<RuleConditionGeneric> getConditions() { return conditions; }
    public void setConditions(LinkedList<RuleConditionGeneric> conditions) { this.conditions = conditions; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of V1 BandwidthControlRule objects into their generic V2 form.
     */
    public static LinkedList<BandwidthControlRuleGeneric> transformRulesToGeneric(List<BandwidthControlRule> v1Rules) {
        LinkedList<BandwidthControlRuleGeneric> out = new LinkedList<>();
        if (v1Rules == null) return out;
        for (BandwidthControlRule rule : v1Rules) {
            out.add(toGeneric(rule));
        }
        return out;
    }


    private static BandwidthControlRuleGeneric toGeneric(BandwidthControlRule rule) {
        BandwidthControlRuleGeneric g = new BandwidthControlRuleGeneric();
        g.setEnabled(rule.getEnabled());
        g.setDescription(rule.getDescription());
        g.setRuleId(String.valueOf(rule.getRuleId()));

        BandwidthControlRuleAction v1Action = rule.getAction();
        if (v1Action != null) {
            BandwidthControlActionGeneric actionGen = new BandwidthControlActionGeneric();
            actionGen.setType(BandwidthControlActionGeneric.fromActionType(v1Action.getActionType()));
            actionGen.setPriority(v1Action.getPriority());
            actionGen.setTagName(v1Action.getTagName());
            actionGen.setTagTimeSec(v1Action.getTagTime());
            actionGen.setQuotaTimeSec(v1Action.getQuotaTime());
            actionGen.setQuotaBytes(v1Action.getQuotaBytes());
            g.setAction(actionGen);
        }

        LinkedList<RuleConditionGeneric> condList = new LinkedList<>();
        if (rule.getConditions() != null) {
            for (BandwidthControlRuleCondition c : rule.getConditions()) {
                RuleConditionGeneric condGen = new RuleConditionGeneric(
                    Boolean.TRUE.equals(c.getInvert()) ? Constants.IS_NOT_EQUALS_TO : Constants.IS_EQUALS_TO,
                    c.getConditionType(),
                    c.getValue()
                );
                condList.add(condGen);
            }
        }
        g.setConditions(condList);
        return g;
    }

    /**
     * Transforms a list of V2 BandwidthControlRuleGeneric objects into V1 BandwidthControlRule objects,
     * preserving existing V1 rules matched by ruleId and removing orphaned rules.
     */
    public static LinkedList<BandwidthControlRule> transformGenericToRules(
            LinkedList<BandwidthControlRuleGeneric> genRules, List<BandwidthControlRule> legacyRules) {
        if (legacyRules == null) legacyRules = new LinkedList<>();

        if (genRules != null) {
            Set<String> incomingIds = genRules.stream()
                    .map(BandwidthControlRuleGeneric::getRuleId)
                    .collect(Collectors.toSet());
            legacyRules.removeIf(r -> !incomingIds.contains(String.valueOf(r.getRuleId())));
        }

        Map<String, BandwidthControlRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(r -> String.valueOf(r.getRuleId()), Function.identity()));

        LinkedList<BandwidthControlRule> out = new LinkedList<>();
        if (genRules != null) {
            for (BandwidthControlRuleGeneric g : genRules) {
                BandwidthControlRule existing = rulesMap.get(g.getRuleId());
                out.add(fromGeneric(g, existing));
            }
        }
        return out;
    }

    private static BandwidthControlRule fromGeneric(BandwidthControlRuleGeneric g, BandwidthControlRule existing) {
        if (existing == null) existing = new BandwidthControlRule();
        existing.setEnabled(g.isEnabled());
        existing.setDescription(g.getDescription());

        if (g.getAction() != null) {
            BandwidthControlRuleAction action = new BandwidthControlRuleAction();
            BandwidthControlRuleAction.ActionType actionType = g.getAction().toActionType();
            if (actionType != null) action.setActionType(actionType);
            if (g.getAction().getPriority() != null) action.setPriority(g.getAction().getPriority());
            if (g.getAction().getTagName() != null) action.setTagName(g.getAction().getTagName());
            if (g.getAction().getTagTimeSec() != null) action.setTagTime(g.getAction().getTagTimeSec());
            if (g.getAction().getQuotaTimeSec() != null) action.setQuotaTime(g.getAction().getQuotaTimeSec());
            if (g.getAction().getQuotaBytes() != null) action.setQuotaBytes(g.getAction().getQuotaBytes());
            existing.setAction(action);
        }

        List<BandwidthControlRuleCondition> conds = new LinkedList<>();
        if (g.getConditions() != null) {
            for (RuleConditionGeneric gc : g.getConditions()) {
                BandwidthControlRuleCondition c = new BandwidthControlRuleCondition(
                    gc.getType(),
                    gc.getValue(),
                    Constants.IS_NOT_EQUALS_TO.equals(gc.getOp())
                );
                conds.add(c);
            }
        }
        existing.setConditions(conds);
        return existing;
    }
}
