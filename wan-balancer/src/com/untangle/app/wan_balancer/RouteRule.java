/**
 * $Id$
 */
package com.untangle.app.wan_balancer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.generic.RuleActionGeneric;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.StringUtil;

/**
 * This in the implementation of a Route Rule
 *
 * A rule is basically a collection of RouteRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class RouteRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<RouteRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private String  description;
    private Integer destinationWan;
    
    public RouteRule() { }

    public RouteRule(boolean enabled, List<RouteRuleCondition> matchers, Integer destinationWan, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setDestinationWan(destinationWan);
        this.setDescription(description);
    }
    
    public List<RouteRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<RouteRuleCondition> matchers ) { this.matchers = matchers; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public Integer getDestinationWan() { return destinationWan; }
    public void setDestinationWan( Integer destinationWan ) { this.destinationWan = destinationWan; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }


    /**
     * Transforms a list of RouteRule into the generic RuleGeneric form for the V2 API.
     * @param v1Rules list of V1 RouteRule objects
     * @return LinkedList of RuleGeneric
     */
    public static LinkedList<RuleGeneric> transformRouteRulesToGeneric(List<RouteRule> v1Rules)
    {
        LinkedList<RuleGeneric> out = new LinkedList<>();
        if (v1Rules == null) return out;
        for (RouteRule rule : v1Rules) {
            out.add(toGeneric(rule));
        }
        return out;
    }

    /**
     * Transforms a single RouteRule into its RuleGeneric representation.
     */
    private static RuleGeneric toGeneric(RouteRule v1)
    {
        boolean enabled = Boolean.TRUE.equals(v1.getEnabled());
        String ruleId = v1.getRuleId() != null ? String.valueOf(v1.getRuleId()) : null;

        RuleActionGeneric action = new RuleActionGeneric();
        action.setType(RuleActionGeneric.Type.DESTINATION_WAN);
        action.setDestinationWan(v1.getDestinationWan());

        LinkedList<RuleConditionGeneric> conds = new LinkedList<>();
        if (v1.getConditions() != null) {
            for (RouteRuleCondition c : v1.getConditions()) {
                String op = Boolean.TRUE.equals(c.getInvert())
                        ? Constants.IS_NOT_EQUALS_TO
                        : Constants.IS_EQUALS_TO;
                conds.add(new RuleConditionGeneric(op, c.getConditionType(), c.getValue()));
            }
        }

        RuleGeneric g = new RuleGeneric(enabled, v1.getDescription(), ruleId);
        g.setAction(action);
        g.setConditions(conds);
        return g;
    }

    /**
     * Transforms a list of generic RuleGeneric into V1 RouteRule, preserving
     * existing V1 rule objects (matched by ruleId) and removing orphaned rules.
     * @param genRules    list of V2 RuleGeneric objects from the UI
     * @param legacyRules current V1 RouteRule list (to preserve state and detect deletions)
     * @return LinkedList of updated/preserved V1 RouteRule objects
     */
    public static LinkedList<RouteRule> transformGenericToRouteRules(
            LinkedList<RuleGeneric> genRules, List<RouteRule> legacyRules)
    {
        if (legacyRules == null) legacyRules = new LinkedList<>();

        RuleGeneric.deleteOrphanRules(
                genRules, legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId()));

        Map<Integer, RouteRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(RouteRule::getRuleId, Function.identity()));

        LinkedList<RouteRule> out = new LinkedList<>();
        if (genRules != null) {
            for (RuleGeneric g : genRules) {
                RouteRule existing = rulesMap.get(StringUtil.getInstance().parseInt(g.getRuleId(), 0));
                out.add(toLegacy(g, existing));
            }
        }
        return out;
    }

    /**
     * Transforms a single RuleGeneric back into a V1 RouteRule, mutating the
     * passed-in existing rule (or creating a new one if null).
     */
    private static RouteRule toLegacy(RuleGeneric g, RouteRule existing)
    {
        if (existing == null) existing = new RouteRule();
        existing.setEnabled(g.isEnabled());
        existing.setDescription(g.getDescription());
        existing.setRuleId(StringUtil.getInstance().parseInt(g.getRuleId(), -1));

        if (g.getAction() != null) {
            existing.setDestinationWan(g.getAction().getDestinationWan());
        }

        List<RouteRuleCondition> conds = new LinkedList<>();
        if (g.getConditions() != null) {
            for (RuleConditionGeneric gc : g.getConditions()) {
                RouteRuleCondition c = new RouteRuleCondition();
                c.setInvert(Constants.IS_NOT_EQUALS_TO.equals(gc.getOp()));
                c.setConditionType(gc.getType());
                c.setValue(gc.getValue());
                conds.add(c);
            }
        }
        existing.setConditions(conds);
        return existing;
    }
}

