/**
 * $Id: WebFilterRule.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.web_filter;

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

import com.untangle.uvm.app.RuleCondition;
import com.untangle.uvm.generic.RuleActionGeneric;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.StringUtil;
import com.untangle.uvm.vnet.AppSession;

/**
 * This in the implementation of a WebFilterRule
 * 
 * A rule is basically a collection of WebFilterRuleConditions and booleans for
 * flagged and blocked to set action to be taken if they match
 */

@SuppressWarnings("serial")
public class WebFilterRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<WebFilterRuleCondition> conditions = null;
    private String description = null;
    public boolean enabled;
    public boolean flagged;
    public boolean blocked;
    private int ruleId;

    public WebFilterRule()
    {
    }

    // THIS IS FOR ECLIPSE - @formatter:off
    
    public List<WebFilterRuleCondition> getConditions() { return this.conditions; }
    public void setConditions(List<WebFilterRuleCondition> conditions) { this.conditions = conditions; }

    public int getRuleId() { return this.ruleId; }
    public void setRuleId(int argval) { this.ruleId = argval; }

    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean argval) { this.enabled = argval; }

    public boolean getFlagged() { return flagged; }
    public void setFlagged(boolean argval) { this.flagged = argval; }

    public boolean getBlocked() { return this.blocked; }
    public void setBlocked(boolean argval) { this.blocked = argval; }
    
    public String getDescription() { return description; }
    public void setDescription(String argstr) { this.description = argstr; }

    // THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of V1 {@link WebFilterRule} objects into the generic
     * {@link RuleGeneric} shape consumed by the Vue UI (V2 API).
     *
     * <p>Action type mapping:
     * <ul>
     *   <li>{@code blocked=true}  &rarr; {@link RuleActionGeneric.Type#REJECT},
     *       {@code action.flagged=true} (blocked implies flagged)</li>
     *   <li>{@code blocked=false} &rarr; {@link RuleActionGeneric.Type#ACCEPT},
     *       {@code action.flagged} reflects the V1 {@code flagged} value</li>
     * </ul>
     * The {@code flagged} property is carried as a dedicated field on
     * {@link RuleActionGeneric} rather than being encoded in the action type,
     * keeping it independent and directly readable by the Vue UI.
     *
     * @param v1Rules V1 filter rule list; returns an empty list if {@code null}
     * @return LinkedList of RuleGeneric
     */
    public static LinkedList<RuleGeneric> transformWebFilterRulesToGeneric(List<WebFilterRule> v1Rules)
    {
        LinkedList<RuleGeneric> out = new LinkedList<>();
        if (v1Rules == null) return out;
        for (WebFilterRule rule : v1Rules) {
            out.add(toGeneric(rule));
        }
        return out;
    }

    /**
     * Transforms a single V1 {@link WebFilterRule} into its {@link RuleGeneric}
     * representation for the V2 API.
     *
     * <p>Action type is {@code REJECT} when the rule blocks traffic, {@code ACCEPT}
     * otherwise. The {@code flagged} property on the action carries the V1
     * {@code flagged} boolean independently of the block/pass decision.
     *
     * @param v1 the V1 rule to convert
     * @return a new RuleGeneric populated from {@code v1}
     */
    private static RuleGeneric toGeneric(WebFilterRule v1)
    {
        String ruleId = String.valueOf(v1.getRuleId());

        RuleActionGeneric action = new RuleActionGeneric();
        action.setType(v1.getBlocked() ? RuleActionGeneric.Type.REJECT : RuleActionGeneric.Type.ACCEPT);
        // flagged is stored as a dedicated field on the action, independent of type
        action.setFlagged(v1.getFlagged());

        LinkedList<RuleConditionGeneric> conds = new LinkedList<>();
        if (v1.getConditions() != null) {
            for (WebFilterRuleCondition c : v1.getConditions()) {
                String op = Boolean.TRUE.equals(c.getInvert())
                        ? Constants.IS_NOT_EQUALS_TO
                        : Constants.IS_EQUALS_TO;
                conds.add(new RuleConditionGeneric(op, c.getConditionType(), c.getValue()));
            }
        }

        RuleGeneric g = new RuleGeneric(v1.getEnabled(), v1.getDescription(), ruleId);
        g.setAction(action);
        g.setConditions(conds);
        return g;
    }

    /**
     * Transforms a list of V2 {@link RuleGeneric} objects back into V1
     * {@link WebFilterRule} objects, preserving existing V1 rule state by
     * ruleId and removing orphaned rules that were deleted in the UI.
     *
     * <p>Action type mapping (reverse of {@link #transformWebFilterRulesToGeneric}):
     * <ul>
     *   <li>{@link RuleActionGeneric.Type#REJECT} &rarr; {@code blocked=true}</li>
     *   <li>{@link RuleActionGeneric.Type#ACCEPT} &rarr; {@code blocked=false}</li>
     *   <li>{@code action.flagged}                &rarr; {@code flagged} (read directly)</li>
     * </ul>
     * Note: {@code _setSettings()} enforces {@code flagged=true} whenever
     * {@code blocked=true}, so that invariant is guaranteed on save regardless
     * of the value sent from the UI.
     *
     * <p>New rules sent from the UI carry a UUID string as their ruleId;
     * {@code _setSettings()} re-assigns sequential integer IDs on save.
     *
     * @param genRules    V2 rule list from the Vue UI
     * @param legacyRules current V1 rule list used for orphan detection and state preservation
     * @return list of updated/preserved V1 WebFilterRule objects
     */
    public static List<WebFilterRule> transformGenericToWebFilterRules(
            LinkedList<RuleGeneric> genRules, List<WebFilterRule> legacyRules)
    {
        if (legacyRules == null) legacyRules = new LinkedList<>();

        // Remove rules deleted in the UI from the legacy list
        RuleGeneric.deleteOrphanRules(
                genRules, legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId()));

        // Map for O(1) lookup of existing rules by ruleId
        Map<Integer, WebFilterRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(WebFilterRule::getRuleId, Function.identity()));

        List<WebFilterRule> out = new LinkedList<>();
        for (RuleGeneric g : genRules) {
            WebFilterRule existing = rulesMap.get(StringUtil.getInstance().parseInt(g.getRuleId(), 0));
            out.add(toLegacy(g, existing));
        }
        return out;
    }

    /**
     * Transforms a single {@link RuleGeneric} back into a V1
     * {@link WebFilterRule}, mutating the passed-in existing rule (or
     * creating a new one if {@code null}).
     *
     * <p>{@code blocked} is derived from the action type ({@code REJECT} → true,
     * {@code ACCEPT} → false). {@code flagged} is read directly from
     * {@link RuleActionGeneric#getFlagged()}; if absent it defaults to
     * {@code false} ({@code _setSettings()} will force it to {@code true}
     * whenever {@code blocked} is {@code true}).
     *
     * @param g        the V2 generic rule from the UI
     * @param existing the matching V1 rule to update, or {@code null} for a new rule
     * @return the populated V1 WebFilterRule
     */
    private static WebFilterRule toLegacy(RuleGeneric g, WebFilterRule existing)
    {
        if (existing == null) existing = new WebFilterRule();
        existing.setEnabled(g.isEnabled());
        existing.setDescription(g.getDescription());
        // New rules from the UI carry a UUID string as ruleId;
        // _setSettings() will assign a real integer ID on save.
        existing.setRuleId(StringUtil.getInstance().parseInt(g.getRuleId(), -1));

        if (g.getAction() != null) {
            existing.setBlocked(g.getAction().getType() == RuleActionGeneric.Type.REJECT);
            // flagged is stored as a dedicated field on the action; default false if absent
            existing.setFlagged(Boolean.TRUE.equals(g.getAction().getFlagged()));
        }

        List<WebFilterRuleCondition> conds = new LinkedList<>();
        if (g.getConditions() != null) {
            for (RuleConditionGeneric gc : g.getConditions()) {
                WebFilterRuleCondition c = new WebFilterRuleCondition();
                c.setInvert(Constants.IS_NOT_EQUALS_TO.equals(gc.getOp()));
                c.setConditionType(gc.getType());
                c.setValue(gc.getValue());
                conds.add(c);
            }
        }
        existing.setConditions(conds);
        return existing;
    }

    public boolean matches(AppSession sess)
    {
        if (!getEnabled()) return false;

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
        for (WebFilterRuleCondition item : conditions) {
            if (!item.matches(sess)) return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
}
