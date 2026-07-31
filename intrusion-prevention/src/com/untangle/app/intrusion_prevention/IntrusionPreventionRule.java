/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention;

import org.json.JSONObject;
import org.json.JSONString;
import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.untangle.uvm.event.generic.EventRuleActionGeneric;
import com.untangle.uvm.event.generic.EventRuleConditionGeneric;
import com.untangle.uvm.event.generic.EventRuleGeneric;

/**
 * Intrusion prevention rule
 */
@SuppressWarnings("serial")
public class IntrusionPreventionRule implements Serializable, JSONString
{
    private String action = "default";
    private List<IntrusionPreventionRuleCondition> conditions = new LinkedList<>();
    private String description = "";
    private Boolean enabled = false;
    private String id = "unid";
    private String sourceNetworks = "recommended";
    private String destinationNetworks = "recommended";

    public IntrusionPreventionRule() { }

    public IntrusionPreventionRule(String action, List<IntrusionPreventionRuleCondition> conditions, String description, Boolean enabled, String id)
    {
        this.action = action;
        this.conditions = conditions;
        this.description = description;
        this.enabled = enabled;
        this.id = id;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public List<IntrusionPreventionRuleCondition> getConditions() { return conditions; }
    public void setConditions(List<IntrusionPreventionRuleCondition> conditions) { this.conditions = conditions; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceNetworks() { return sourceNetworks; }
    public void setSourceNetworks(String sourceNetworks) { this.sourceNetworks = sourceNetworks; }

    public String getDestinationNetworks() { return destinationNetworks; }
    public void setDestinationNetworks(String destinationNetworks) { this.destinationNetworks = destinationNetworks; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of IntrusionPreventionRule into the generic EventRuleGeneric form for the V2 API.
     * @param v1Rules list of V1 IntrusionPreventionRule objects
     * @return LinkedList of EventRuleGeneric
     */
    public static LinkedList<EventRuleGeneric> transformIpRulesToGeneric(List<IntrusionPreventionRule> v1Rules)
    {
        LinkedList<EventRuleGeneric> out = new LinkedList<>();
        if (v1Rules == null) return out;
        for (IntrusionPreventionRule rule : v1Rules) {
            out.add(toGeneric(rule));
        }
        return out;
    }

    /**
     * Transforms a single IntrusionPreventionRule into its EventRuleGeneric representation.
     */
    private static EventRuleGeneric toGeneric(IntrusionPreventionRule rule)
    {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(rule.getEnabled());
        String ruleId = rule.getId();

        // Transform Action
        EventRuleActionGeneric ruleActionGen = new EventRuleActionGeneric();
        ruleActionGen.setType(ipsActionStringToType(rule.getAction()));
        ruleActionGen.setSourceNetworks(rule.getSourceNetworks());
        ruleActionGen.setDestinationNetworks(rule.getDestinationNetworks());

        // Transform Conditions
        LinkedList<EventRuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        if (rule.getConditions() != null) {
            for (IntrusionPreventionRuleCondition c : rule.getConditions()) {
                EventRuleConditionGeneric ruleConditionGen = new EventRuleConditionGeneric(c.getComparator(), c.getType(), c.getValue());
                ruleConditionGenList.add(ruleConditionGen);
            }
        }

        EventRuleGeneric ipRuleGen = new EventRuleGeneric(enabled, rule.getDescription(), ruleId);
        ipRuleGen.setAction(ruleActionGen);
        ipRuleGen.setConditions(ruleConditionGenList);

        return ipRuleGen;
    }

    private static EventRuleActionGeneric.Type ipsActionStringToType(String action)
    {
        if (action == null) return EventRuleActionGeneric.Type.IPS_DEFAULT;
        switch (action) {
            case "log":       return EventRuleActionGeneric.Type.IPS_LOG;
            case "blocklog":  return EventRuleActionGeneric.Type.IPS_BLOCKLOG;
            case "block":     return EventRuleActionGeneric.Type.IPS_BLOCK;
            case "disable":   return EventRuleActionGeneric.Type.IPS_DISABLE;
            case "whitelist": return EventRuleActionGeneric.Type.IPS_WHITELIST;
            default:          return EventRuleActionGeneric.Type.IPS_DEFAULT;
        }
    }

    /**
     * Transforms a list of EventRuleGeneric objects into V1 IntrusionPreventionRule objects,
     * preserving existing V1 rules (matched by id) and removing orphaned rules.
     * @param genRules    list of V2 EventRuleGeneric objects from the UI
     * @param legacyRules current V1 IntrusionPreventionRule list
     * @return LinkedList of updated/preserved V1 IntrusionPreventionRule objects
     */
    public static LinkedList<IntrusionPreventionRule> transformGenericToIpRules(
            LinkedList<EventRuleGeneric> genRules, List<IntrusionPreventionRule> legacyRules)
    {
        if (legacyRules == null) legacyRules = new LinkedList<>();

        deleteOrphanIpRules(genRules, legacyRules);

        Map<String, IntrusionPreventionRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(IntrusionPreventionRule::getId, Function.identity()));

        LinkedList<IntrusionPreventionRule> out = new LinkedList<>();
        if (genRules != null) {
            for (EventRuleGeneric g : genRules) {
                IntrusionPreventionRule existing = rulesMap.get(g.getRuleId());
                out.add(fromGeneric(g, existing));
            }
        }
        return out;
    }

    /**
     * Transforms a single EventRuleGeneric back into a V1 IntrusionPreventionRule.
     * @param g        the generic rule from the UI
     * @param existing the current V1 rule (null for new rules)
     * @return populated V1 IntrusionPreventionRule
     */
    private static IntrusionPreventionRule fromGeneric(EventRuleGeneric g, IntrusionPreventionRule existing)
    {
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
            for (EventRuleConditionGeneric gc : g.getConditions()) {
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

    /**
     * Maps an {@link EventRuleActionGeneric.Type} enum value back to the IPS V1 action string.
     *
     * @param type the generic action type; {@code null} is treated as the default action.
     * @return the V1 action string: {@code "log"}, {@code "blocklog"}, {@code "block"},
     *         {@code "disable"}, {@code "whitelist"}, or {@code "default"}.
     */
    private static String ipsTypeToActionString(EventRuleActionGeneric.Type type)
    {
        if (type == null) return "default";
        switch (type) {
            case IPS_LOG:       return "log";
            case IPS_BLOCKLOG:  return "blocklog";
            case IPS_BLOCK:     return "block";
            case IPS_DISABLE:   return "disable";
            case IPS_WHITELIST: return "whitelist";
            default:            return "default";
        }
    }

    private static void deleteOrphanIpRules(LinkedList<EventRuleGeneric> newRules, List<IntrusionPreventionRule> legacyRules)
    {
        if (newRules == null) return;
        Set<String> incomingIds = newRules.stream()
                .map(EventRuleGeneric::getRuleId)
                .collect(Collectors.toSet());
        legacyRules.removeIf(r -> !incomingIds.contains(r.getId()));
    }
}
