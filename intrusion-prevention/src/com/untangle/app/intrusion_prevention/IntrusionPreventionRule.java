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
import java.util.function.Function;
import java.util.stream.Collectors;

import com.untangle.uvm.generic.RuleActionGeneric;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;

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
     * Transforms a list of IntrusionPreventionRule into the generic RuleGeneric form for the V2 API.
     * @param v1Rules list of V1 IntrusionPreventionRule objects
     * @return LinkedList of RuleGeneric
     */
    public static LinkedList<RuleGeneric> transformIpRulesToGeneric(List<IntrusionPreventionRule> v1Rules)
    {
        LinkedList<RuleGeneric> out = new LinkedList<>();
        if (v1Rules == null) return out;
        for (IntrusionPreventionRule rule : v1Rules) {
            out.add(toGeneric(rule));
        }
        return out;
    }

    /**
     * Transforms a single IntrusionPreventionRule into its RuleGeneric representation.
     */
    private static RuleGeneric toGeneric(IntrusionPreventionRule v1)
    {
        RuleActionGeneric action = new RuleActionGeneric();
        action.setType(ipsActionStringToType(v1.getAction()));

        LinkedList<RuleConditionGeneric> conds = new LinkedList<>();
        if (v1.getConditions() != null) {
            for (IntrusionPreventionRuleCondition c : v1.getConditions()) {
                String op = "!=".equals(c.getComparator())
                        ? Constants.IS_NOT_EQUALS_TO
                        : Constants.IS_EQUALS_TO;
                RuleConditionGeneric gc = new RuleConditionGeneric();
                gc.setOp(op);
                gc.setTypeString(c.getType());
                gc.setValue(c.getValue());
                conds.add(gc);
            }
        }

        RuleGeneric g = new RuleGeneric(Boolean.TRUE.equals(v1.getEnabled()), v1.getDescription(), v1.getId());
        g.setAction(action);
        g.setConditions(conds);
        g.setSourceNetworks(v1.getSourceNetworks());
        g.setDestinationNetworks(v1.getDestinationNetworks());
        return g;
    }

    /**
     * Transforms a list of generic RuleGeneric into V1 IntrusionPreventionRule, preserving
     * existing V1 rule objects (matched by id) and removing orphaned rules.
     * @param genRules    list of V2 RuleGeneric objects from the UI
     * @param legacyRules current V1 IntrusionPreventionRule list
     * @return LinkedList of updated/preserved V1 IntrusionPreventionRule objects
     */
    public static LinkedList<IntrusionPreventionRule> transformGenericToIpRules(
            LinkedList<RuleGeneric> genRules, List<IntrusionPreventionRule> legacyRules)
    {
        if (legacyRules == null) legacyRules = new LinkedList<>();

        RuleGeneric.deleteOrphanRules(
                genRules, legacyRules,
                RuleGeneric::getRuleId,
                IntrusionPreventionRule::getId);

        Map<String, IntrusionPreventionRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(IntrusionPreventionRule::getId, Function.identity()));

        LinkedList<IntrusionPreventionRule> out = new LinkedList<>();
        if (genRules != null) {
            for (RuleGeneric g : genRules) {
                IntrusionPreventionRule existing = rulesMap.get(g.getRuleId());
                out.add(toLegacy(g, existing));
            }
        }
        return out;
    }

    /**
     * Transforms a single RuleGeneric back into a V1 IntrusionPreventionRule.
     */
    private static IntrusionPreventionRule toLegacy(RuleGeneric g, IntrusionPreventionRule existing)
    {
        if (existing == null) existing = new IntrusionPreventionRule();
        existing.setEnabled(g.isEnabled());
        existing.setDescription(g.getDescription());
        existing.setId(g.getRuleId());

        if (g.getAction() != null)
            existing.setAction(ipsTypeToActionString(g.getAction().getType()));

        if (g.getSourceNetworks() != null)
            existing.setSourceNetworks(g.getSourceNetworks());
        if (g.getDestinationNetworks() != null)
            existing.setDestinationNetworks(g.getDestinationNetworks());

        List<IntrusionPreventionRuleCondition> conds = new LinkedList<>();
        if (g.getConditions() != null) {
            for (RuleConditionGeneric gc : g.getConditions()) {
                IntrusionPreventionRuleCondition c = new IntrusionPreventionRuleCondition();
                c.setType(gc.getTypeString());
                c.setComparator(Constants.IS_NOT_EQUALS_TO.equals(gc.getOp()) ? "!=" : "=");
                c.setValue(gc.getValue());
                conds.add(c);
            }
        }
        existing.setConditions(conds);
        return existing;
    }

    private static RuleActionGeneric.Type ipsActionStringToType(String action)
    {
        if (action == null) return RuleActionGeneric.Type.IPS_DEFAULT;
        switch (action) {
            case "log":       return RuleActionGeneric.Type.IPS_LOG;
            case "blocklog":  return RuleActionGeneric.Type.IPS_BLOCKLOG;
            case "block":     return RuleActionGeneric.Type.IPS_BLOCK;
            case "disable":   return RuleActionGeneric.Type.IPS_DISABLE;
            case "whitelist": return RuleActionGeneric.Type.IPS_WHITELIST;
            default:          return RuleActionGeneric.Type.IPS_DEFAULT;
        }
    }

    private static String ipsTypeToActionString(RuleActionGeneric.Type type)
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
}

