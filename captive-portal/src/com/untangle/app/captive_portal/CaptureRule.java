/**
 * $Id$
 */

package com.untangle.app.captive_portal;

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
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.SessionAttachments;

/**
 * This in the implementation of a Capture Rule
 * 
 * A rule is basically a collection of CaptureRuleConditions (matchers) and what
 * to do if the matchers match (capture, log, etc)
 * 
 * @author mahotz
 * 
 */

@SuppressWarnings("serial")
public class CaptureRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<CaptureRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean capture;
    private String description;

    public CaptureRule()
    {
    }

    public CaptureRule(boolean enabled, List<CaptureRuleCondition> matchers, boolean capture, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setCapture(Boolean.valueOf(capture));
        this.setDescription(description);
    }

// THIS IS FOR ECLIPSE - @formatter:off
    
    public List<CaptureRuleCondition> getConditions() { return this.matchers; }
    public void setConditions(List<CaptureRuleCondition> matchers) { this.matchers = matchers; }

    // NOTE: Use getRuleId and setRuleId instead. Kept for backwards compatability only.
    public Integer getId() { return this.ruleId; }
    public void setId(Integer ruleId) { this.ruleId = ruleId; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public Boolean getCapture() { return capture; }
    public void setCapture(Boolean capture) { this.capture = capture; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * @return A String of this rule in JSON format
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Checks to see if a connection matches this rule
     * 
     * @param protocol
     *        The network protocol
     * @param srcIntf
     *        The source interface
     * @param dstIntf
     *        The destination interface
     * @param srcAddress
     *        The source address
     * @param dstAddress
     *        The destination address
     * @param srcPort
     *        The source port
     * @param dstPort
     *        The destination port
     * @param attachments
     *        attachments
     * @return True if this rule matches, otherwise false
     */
    public boolean isMatch(short protocol, int srcIntf, int dstIntf, InetAddress srcAddress, InetAddress dstAddress, int srcPort, int dstPort, SessionAttachments attachments)
    {
        if (!getEnabled()) return false;

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
        for (CaptureRuleCondition matcher : matchers) {
            if (!matcher.matches(protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort, attachments)) return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }

    /**
     * Checks to see if a session matches this rule.
     * 
     * @param sess
     *        The session object
     * @return True if this rule matches, otherwise false
     */
    public boolean isMatch(AppSession sess)
    {
        if (!getEnabled()) return false;

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
        for (CaptureRuleCondition matcher : matchers) {
            if (!matcher.matches(sess)) return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }

    /**
     * Transforms a list of CaptureRule into the generic RuleGeneric form for
     * the V2 API. Used by getSettingsV2().
     *
     * @param v1Rules list of V1 CaptureRule objects
     * @return LinkedList of RuleGeneric
     */
    public static LinkedList<RuleGeneric> transformCaptureRulesToGeneric(List<CaptureRule> v1Rules)
    {
        LinkedList<RuleGeneric> out = new LinkedList<>();
        if (v1Rules == null) return out;
        for (CaptureRule rule : v1Rules) {
            out.add(toGeneric(rule));
        }
        return out;
    }

    /**
     * Transforms a single CaptureRule into its RuleGeneric representation.
     */
    private static RuleGeneric toGeneric(CaptureRule v1)
    {
        boolean enabled = Boolean.TRUE.equals(v1.getEnabled());
        String ruleId = v1.getRuleId() != null ? String.valueOf(v1.getRuleId()) : null;

        RuleActionGeneric action = new RuleActionGeneric();
        action.setType(Boolean.TRUE.equals(v1.getCapture())
                ? RuleActionGeneric.Type.CAPTURE
                : RuleActionGeneric.Type.PASS);

        LinkedList<RuleConditionGeneric> conds = new LinkedList<>();
        if (v1.getConditions() != null) {
            for (CaptureRuleCondition c : v1.getConditions()) {
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
     * Transforms a list of generic RuleGeneric into V1 CaptureRule, preserving
     * existing V1 rule objects (matched by ruleId) and removing orphaned rules.
     * Used by setSettingsV2().
     *
     * @param genRules    list of V2 RuleGeneric objects from the UI
     * @param legacyRules current V1 CaptureRule list (to preserve internal state
     *                    on update and detect deletions)
     * @return LinkedList of updated/preserved V1 CaptureRule objects
     */
    public static LinkedList<CaptureRule> transformGenericToCaptureRules(
            LinkedList<RuleGeneric> genRules, List<CaptureRule> legacyRules)
    {
        if (legacyRules == null) legacyRules = new LinkedList<>();

        // Remove rules deleted in UI from the legacy list
        RuleGeneric.deleteOrphanRules(
                genRules, legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId()));

        // Map for O(1) lookup of existing rules by ruleId
        Map<Integer, CaptureRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(CaptureRule::getRuleId, Function.identity()));

        LinkedList<CaptureRule> out = new LinkedList<>();
        if (genRules != null) {
            for (RuleGeneric g : genRules) {
                CaptureRule existing = rulesMap.get(StringUtil.getInstance().parseInt(g.getRuleId(), 0));
                out.add(toLegacy(g, existing));
            }
        }
        return out;
    }

    /**
     * Transforms a single RuleGeneric back into a V1 CaptureRule, mutating the
     * passed-in existing rule (or creating a new one if null).
     */
    private static CaptureRule toLegacy(RuleGeneric g, CaptureRule existing)
    {
        if (existing == null) existing = new CaptureRule();
        existing.setEnabled(g.isEnabled());
        existing.setDescription(g.getDescription());
        // For new rules from UI, ruleId is a UUID string -> parseInt returns -1;
        // CaptivePortalApp.saveAppSettings() will assign a real integer ID on save.
        existing.setRuleId(StringUtil.getInstance().parseInt(g.getRuleId(), -1));

        if (g.getAction() != null) {
            existing.setCapture(g.getAction().getType() == RuleActionGeneric.Type.CAPTURE);
        }

        List<CaptureRuleCondition> conds = new LinkedList<>();
        if (g.getConditions() != null) {
            for (RuleConditionGeneric gc : g.getConditions()) {
                CaptureRuleCondition c = new CaptureRuleCondition();
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
