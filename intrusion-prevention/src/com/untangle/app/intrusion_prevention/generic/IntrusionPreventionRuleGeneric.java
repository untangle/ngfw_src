/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention.generic;

import com.untangle.app.intrusion_prevention.IntrusionPreventionRule;
import com.untangle.app.intrusion_prevention.IntrusionPreventionRuleCondition;
import com.untangle.uvm.event.generic.EventRuleConditionGeneric;

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
 * Generic (V2) rule for Intrusion Prevention, used for Vue UI transformations.
 */
@SuppressWarnings("serial")
public class IntrusionPreventionRuleGeneric implements JSONString, Serializable {

    public IntrusionPreventionRuleGeneric() {}

    public IntrusionPreventionRuleGeneric(boolean enabled, String description, String ruleId) {
        this.enabled = enabled;
        this.description = description;
        this.ruleId = ruleId;
    }

    private boolean enabled;
    private String description;
    private String ruleId;
    private IntrusionPreventionActionGeneric action;
    private LinkedList<IntrusionPreventionConditionGeneric> conditions;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public IntrusionPreventionActionGeneric getAction() { return action; }
    public void setAction(IntrusionPreventionActionGeneric action) { this.action = action; }
    public LinkedList<IntrusionPreventionConditionGeneric> getConditions() { return conditions; }
    public void setConditions(LinkedList<IntrusionPreventionConditionGeneric> conditions) { this.conditions = conditions; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of IntrusionPreventionRule into the generic V2 form for the Vue UI.
     * @param v1Rules list of V1 IntrusionPreventionRule objects
     * @return LinkedList of IntrusionPreventionRuleGeneric
     */
    public static LinkedList<IntrusionPreventionRuleGeneric> transformIpRulesToGeneric(List<IntrusionPreventionRule> v1Rules) {
        LinkedList<IntrusionPreventionRuleGeneric> out = new LinkedList<>();
        if (v1Rules == null) return out;
        for (IntrusionPreventionRule rule : v1Rules) {
            out.add(toGeneric(rule));
        }
        return out;
    }

    private static IntrusionPreventionRuleGeneric toGeneric(IntrusionPreventionRule rule) {
        IntrusionPreventionActionGeneric action = new IntrusionPreventionActionGeneric();
        // Transform Action
        action.setType(ipsActionStringToType(rule.getAction()));
        action.setSourceNetworks(rule.getSourceNetworks());
        action.setDestinationNetworks(rule.getDestinationNetworks());

        // Transform Conditions
        LinkedList<IntrusionPreventionConditionGeneric> condList = new LinkedList<>();
        if (rule.getConditions() != null) {
            for (IntrusionPreventionRuleCondition c : rule.getConditions()) {
                IntrusionPreventionConditionGeneric ruleConditionGen = new IntrusionPreventionConditionGeneric(c.getComparator(), c.getType(), c.getValue());
                condList.add(ruleConditionGen);
            }
        }

        IntrusionPreventionRuleGeneric ruleGen = new IntrusionPreventionRuleGeneric(
                Boolean.TRUE.equals(rule.getEnabled()), rule.getDescription(), rule.getId());
        ruleGen.setAction(action);
        ruleGen.setConditions(condList);
        return ruleGen;
    }

    private static IntrusionPreventionActionGeneric.Type ipsActionStringToType(String action) {
        if (action == null) return IntrusionPreventionActionGeneric.Type.DEFAULT;
        switch (action) {
            case "log":       return IntrusionPreventionActionGeneric.Type.LOG;
            case "blocklog":  return IntrusionPreventionActionGeneric.Type.BLOCKLOG;
            case "block":     return IntrusionPreventionActionGeneric.Type.BLOCK;
            case "disable":   return IntrusionPreventionActionGeneric.Type.DISABLE;
            case "whitelist": return IntrusionPreventionActionGeneric.Type.WHITELIST;
            default:          return IntrusionPreventionActionGeneric.Type.DEFAULT;
        }
    }

    /**
     * Transforms a list of IntrusionPreventionRuleGeneric objects into V1 IntrusionPreventionRule objects,
     * preserving existing V1 rules (matched by id) and removing orphaned rules.
     * @param genRules    list of V2 IntrusionPreventionRuleGeneric objects from the UI
     * @param legacyRules current V1 IntrusionPreventionRule list
     * @return LinkedList of updated/preserved V1 IntrusionPreventionRule objects
     */
    public static LinkedList<IntrusionPreventionRule> transformGenericToIpRules(
            LinkedList<IntrusionPreventionRuleGeneric> genRules, List<IntrusionPreventionRule> legacyRules) {
        if (legacyRules == null) legacyRules = new LinkedList<>();

        deleteOrphanIpRules(genRules, legacyRules);

        Map<String, IntrusionPreventionRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(IntrusionPreventionRule::getId, Function.identity()));

        LinkedList<IntrusionPreventionRule> out = new LinkedList<>();
        if (genRules != null) {
            for (IntrusionPreventionRuleGeneric g : genRules) {
                IntrusionPreventionRule existing = rulesMap.get(g.getRuleId());
                out.add(fromGeneric(g, existing));
            }
        }
        return out;
    }

    private static IntrusionPreventionRule fromGeneric(IntrusionPreventionRuleGeneric g, IntrusionPreventionRule existing) {
        if (existing == null) existing = new IntrusionPreventionRule();
        existing.setEnabled(g.isEnabled());
        existing.setDescription(g.getDescription());
        existing.setId(g.getRuleId());

        if (g.getAction() != null) {
            existing.setAction(ipsTypeToActionString(g.getAction().getType()));
            if (g.getAction().getSourceNetworks() != null)
                existing.setSourceNetworks(g.getAction().getSourceNetworks());
            if (g.getAction().getDestinationNetworks() != null)
                existing.setDestinationNetworks(g.getAction().getDestinationNetworks());
        }

        List<IntrusionPreventionRuleCondition> conds = new LinkedList<>();
        if (g.getConditions() != null) {
            for (IntrusionPreventionConditionGeneric gc : g.getConditions()) {
                IntrusionPreventionRuleCondition c = new IntrusionPreventionRuleCondition();
                c.setType(gc.getType());
                c.setComparator(gc.getOp() != null ? gc.getOp() : "=");
                c.setValue(gc.getValue());
                conds.add(c);
            }
        }
        existing.setConditions(conds);
        return existing;
    }

    private static String ipsTypeToActionString(IntrusionPreventionActionGeneric.Type type) {
        if (type == null) return "default";
        switch (type) {
            case LOG:       return "log";
            case BLOCKLOG:  return "blocklog";
            case BLOCK:     return "block";
            case DISABLE:   return "disable";
            case WHITELIST: return "whitelist";
            default:        return "default";
        }
    }

    private static void deleteOrphanIpRules(LinkedList<IntrusionPreventionRuleGeneric> newRules, List<IntrusionPreventionRule> legacyRules) {
        if (newRules == null) return;
        Set<String> incomingIds = newRules.stream()
                .map(IntrusionPreventionRuleGeneric::getRuleId)
                .collect(Collectors.toSet());
        legacyRules.removeIf(r -> !incomingIds.contains(r.getId()));
    }
}
