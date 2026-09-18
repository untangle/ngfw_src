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
     * Transforms a list of WebFilterRule into the generic RuleGeneric form for
     * the V2 API. Used by getSettingsV2().
     *
     * @param v1Rules list of V1 WebFilterRule objects
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
     * Transforms a single WebFilterRule into its RuleGeneric representation.
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
     * Transforms a list of generic RuleGeneric into V1 WebFilterRule, preserving
     * existing V1 rule objects (matched by ruleId) and removing orphaned rules.
     * Used by setSettingsV2().
     *
     * @param genRules    list of V2 RuleGeneric objects from the UI
     * @param legacyRules current V1 WebFilterRule list (to preserve internal state
     *                    on update and detect deletions)
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
     * Transforms a single RuleGeneric back into a V1 WebFilterRule, mutating the
     * passed-in existing rule (or creating a new one if null).
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
