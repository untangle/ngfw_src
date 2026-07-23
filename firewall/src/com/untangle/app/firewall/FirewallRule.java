/**
 * $Id$
 */
package com.untangle.app.firewall;

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

/**
 * This in the implementation of a Firewall Rule
 *
 * A rule is basically a collection of FirewallRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class FirewallRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<FirewallRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean flag;
    private Boolean block;
    private String description;
    
    public FirewallRule()
    {
    }

    public FirewallRule(boolean enabled, List<FirewallRuleCondition> matchers, boolean flag, boolean block, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setFlag(Boolean.valueOf(flag));
        this.setBlock(Boolean.valueOf(block));
        this.setDescription(description);
    }
    
    public List<FirewallRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<FirewallRuleCondition> newValue ) { this.matchers = newValue; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public Boolean getBlock() { return block; }
    public void setBlock( Boolean newValue ) { this.block = newValue; }

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
         * IF any matcher doesn't match - return false
         */
        for ( FirewallRuleCondition matcher : matchers ) {
            if (!matcher.matches(protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort, attachments))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }

    /**
     * Transforms a list of FirewallRule into the generic RuleGeneric form for
     * the V2 API. Used by getSettingsV2().
     *
     * @param v1Rules list of V1 FirewallRule objects
     * @return LinkedList of RuleGeneric
     */
    public static LinkedList<RuleGeneric> transformFirewallRulesToGeneric(List<FirewallRule> v1Rules)
    {
        LinkedList<RuleGeneric> out = new LinkedList<>();
        if (v1Rules == null) return out;
        for (FirewallRule rule : v1Rules) {
            out.add(toGeneric(rule));
        }
        return out;
    }

    /**
     * Transforms a single FirewallRule into its RuleGeneric representation.
     */
    private static RuleGeneric toGeneric(FirewallRule v1)
    {
        String ruleId = String.valueOf(v1.getRuleId());

        RuleActionGeneric action = new RuleActionGeneric();
        action.setType(Boolean.TRUE.equals(v1.getBlock()) ? RuleActionGeneric.Type.REJECT : RuleActionGeneric.Type.ACCEPT);
        action.setFlagged(v1.getFlag());

        LinkedList<RuleConditionGeneric> conds = new LinkedList<>();
        if (v1.getConditions() != null) {
            for (FirewallRuleCondition c : v1.getConditions()) {
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
     * Transforms a list of generic RuleGeneric into V1 FirewallRule, preserving
     * existing V1 rule objects (matched by ruleId) and removing orphaned rules.
     * Used by setSettingsV2().
     *
     * @param genRules    list of V2 RuleGeneric objects from the UI
     * @param legacyRules current V1 FirewallRule list (to preserve internal state
     *                    on update and detect deletions)
     * @return list of updated/preserved V1 FirewallRule objects
     */
    public static List<FirewallRule> transformGenericToFirewallRules(
            LinkedList<RuleGeneric> genRules, List<FirewallRule> legacyRules)
    {
        if (legacyRules == null) legacyRules = new LinkedList<>();

        RuleGeneric.deleteOrphanRules(
                genRules, legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId()));

        Map<Integer, FirewallRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(FirewallRule::getRuleId, Function.identity()));

        List<FirewallRule> out = new LinkedList<>();
        for (RuleGeneric g : genRules) {
            FirewallRule existing = rulesMap.get(StringUtil.getInstance().parseInt(g.getRuleId(), 0));
            out.add(toLegacy(g, existing));
        }
        return out;
    }

    /**
     * Transforms a single RuleGeneric back into a V1 FirewallRule, mutating the
     * passed-in existing rule (or creating a new one if null).
     */
    private static FirewallRule toLegacy(RuleGeneric g, FirewallRule existing)
    {
        if (existing == null) existing = new FirewallRule();
        existing.setEnabled(g.isEnabled());
        existing.setDescription(g.getDescription());
        existing.setRuleId(StringUtil.getInstance().parseInt(g.getRuleId(), -1));

        if (g.getAction() != null) {
            existing.setBlock(g.getAction().getType() == RuleActionGeneric.Type.REJECT);
            existing.setFlag(Boolean.TRUE.equals(g.getAction().getFlagged()));
        }

        List<FirewallRuleCondition> conds = new LinkedList<>();
        if (g.getConditions() != null) {
            for (RuleConditionGeneric gc : g.getConditions()) {
                FirewallRuleCondition c = new FirewallRuleCondition();
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

