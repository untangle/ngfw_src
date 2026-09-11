/**
 * $Id$
 */
package com.untangle.app.threat_prevention;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.generic.RuleActionGeneric;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.StringUtil;
import com.untangle.uvm.vnet.SessionAttachments;
import com.untangle.uvm.vnet.AppSession;

/**
 * This in the implementation of an Threat Prevention Action Rule
 *
 * A rule is basically a collection of ThreatPreventionRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class ThreatPreventionRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<ThreatPreventionRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean flag;
    private String action;
    private String description;
    
    public ThreatPreventionRule()
    {
    }

    public ThreatPreventionRule(boolean enabled, List<ThreatPreventionRuleCondition> matchers, boolean flag, String action, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setFlag(Boolean.valueOf(flag));
        this.setAction(action);
        this.setDescription(description);
    }
    
    public List<ThreatPreventionRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<ThreatPreventionRuleCondition> newValue ) { this.matchers = newValue; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public String getAction() { return action; }
    public void setAction( String newValue ) { this.action = newValue; }

    public Boolean getFlag() { return flag; }
    public void setFlag( Boolean newValue ) { this.flag = newValue; }
    
    public String getDescription() { return description; }
    public void setDescription( String newValue ) { this.description = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
    public boolean isMatch( short protocol,
                            int srcIntf, int dstIntf,
                            InetAddress srcAddress, InetAddress dstAddress,
                            int srcPort, int dstPort,
                            SessionAttachments attachments)
    {
        if (!getEnabled())
            return false;

        //logger.debug("Checking rule " + getRuleId() + " against [" + protocol + " " + srcAddress + ":" + srcPort + " -> " + dstAddress + ":" + dstPort + "]");
            
        /**
         * If no matchers return true
         */
        if (this.matchers == null) {
            logger.warn("Null matchers - assuming true");
            return true;
        }

        /**
         * It everything doesn't match, then return false.
         */
        for ( ThreatPreventionRuleCondition matcher : matchers ) {
            if (!matcher.matches(protocol,
                            srcIntf, dstIntf,
                            srcAddress, dstAddress,
                            srcPort, dstPort,
                            attachments) ){

                return false;
            }
        }

        /**
         * Otherwise these all match.
         */
        return true;
    }

    public boolean isMatch( AppSession session)
    {
        if (!getEnabled())
            return false;

        //logger.debug("Checking rule " + getRuleId() + " against [" + protocol + " " + srcAddress + ":" + srcPort + " -> " + dstAddress + ":" + dstPort + "]");
            
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
        for (ThreatPreventionRuleCondition item : matchers) {
            if (!item.matches(session)) return false;
        }

        /**
         * Otherwise everything is matching.
         */
        return true;
    }

    /**
     * Transforms a list of ThreatPreventionRule into the generic RuleGeneric form for
     * the V2 API. Used by getSettingsV2().
     *
     * @param v1Rules list of V1 ThreatPreventionRule objects
     * @return LinkedList of RuleGeneric
     */
    public static LinkedList<RuleGeneric> transformThreatPreventionRulesToGeneric(List<ThreatPreventionRule> v1Rules)
    {
        LinkedList<RuleGeneric> out = new LinkedList<>();
        if (v1Rules == null) return out;
        for (ThreatPreventionRule rule : v1Rules) {
            out.add(toGeneric(rule));
        }
        return out;
    }

    /**
     * Transforms a single ThreatPreventionRule into its RuleGeneric representation.
     */
    private static RuleGeneric toGeneric(ThreatPreventionRule v1)
    {
        if (v1 == null) return null;
        String ruleId = String.valueOf(v1.getRuleId());

        RuleActionGeneric action = new RuleActionGeneric();
        action.setType(ThreatPreventionApp.ACTION_BLOCK.equals(v1.getAction()) ? RuleActionGeneric.Type.REJECT : RuleActionGeneric.Type.ACCEPT);
        action.setFlagged(v1.getFlag());

        LinkedList<RuleConditionGeneric> conds = new LinkedList<>();
        if (v1.getConditions() != null) {
            for (ThreatPreventionRuleCondition c : v1.getConditions()) {
                String op = Boolean.TRUE.equals(c.getInvert())
                        ? Constants.IS_NOT_EQUALS_TO
                        : Constants.IS_EQUALS_TO;
                conds.add(new RuleConditionGeneric(op, c.getConditionType(), c.getValue()));
            }
        }

        RuleGeneric g = new RuleGeneric(Boolean.TRUE.equals(v1.getEnabled()), v1.getDescription(), ruleId);
        g.setAction(action);
        g.setConditions(conds);
        return g;
    }

    /**
     * Transforms a list of generic RuleGeneric into V1 ThreatPreventionRule, preserving
     * existing V1 rule objects (matched by ruleId) and removing orphaned rules.
     * Used by setSettingsV2().
     *
     * @param genRules    list of V2 RuleGeneric objects from the UI
     * @param legacyRules current V1 ThreatPreventionRule list (to preserve internal state
     *                    on update and detect deletions)
     * @return list of updated/preserved V1 ThreatPreventionRule objects
     */
    public static List<ThreatPreventionRule> transformGenericToThreatPreventionRules(
            LinkedList<RuleGeneric> genRules, List<ThreatPreventionRule> legacyRules)
    {
        if (legacyRules == null) legacyRules = new LinkedList<>();

        RuleGeneric.deleteOrphanRules(
                genRules, legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId()));

        Map<Integer, ThreatPreventionRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(ThreatPreventionRule::getRuleId, Function.identity()));

        List<ThreatPreventionRule> out = new LinkedList<>();
        for (RuleGeneric g : genRules) {
            ThreatPreventionRule existing = rulesMap.get(StringUtil.getInstance().parseInt(g.getRuleId(), 0));
            out.add(toLegacy(g, existing));
        }
        return out;
    }

    /**
     * Transforms a single RuleGeneric back into a V1 ThreatPreventionRule, mutating the
     * passed-in existing rule (or creating a new one if null).
     */
    private static ThreatPreventionRule toLegacy(RuleGeneric g, ThreatPreventionRule existing)
    {
        if (g == null) return existing;
        if (existing == null) existing = new ThreatPreventionRule();
        existing.setEnabled(g.isEnabled());
        existing.setDescription(g.getDescription());
        existing.setRuleId(StringUtil.getInstance().parseInt(g.getRuleId(), -1));

        if (g.getAction() != null) {
            existing.setAction(g.getAction().getType() == RuleActionGeneric.Type.REJECT
                    ? ThreatPreventionApp.ACTION_BLOCK
                    : ThreatPreventionApp.ACTION_PASS);
            existing.setFlag(Boolean.TRUE.equals(g.getAction().getFlagged()));
        }

        List<ThreatPreventionRuleCondition> conds = new LinkedList<>();
        if (g.getConditions() != null) {
            for (RuleConditionGeneric gc : g.getConditions()) {
                ThreatPreventionRuleCondition c = new ThreatPreventionRuleCondition();
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

