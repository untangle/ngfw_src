/**
 * $Id: UpnpRule.java 41565 2016-07-27 22:13:52Z cblaise $
 */
package com.untangle.uvm.network;

import java.util.LinkedList;
import java.util.List;
import java.io.Serializable;

import com.untangle.uvm.generic.RuleActionGeneric;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This in the implementation of a Upnp Rule
 *
 * A rule is basically a collection of UpnpRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class UpnpRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<UpnpRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private String description;
    private Boolean allow;

    public UpnpRule() { }

    public UpnpRule(boolean enabled, String description, List<UpnpRuleCondition> matchers, Boolean allow)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setAllow(Boolean.valueOf(allow));
    }

    public List<UpnpRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<UpnpRuleCondition> matchers ) { this.matchers = matchers; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public Boolean getAllow() { return allow; }
    public void setAllow( Boolean allow ) { this.allow = allow; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of UpnpRule objects into their generic RuleGeneric representation.
     * Used for get api calls
     * @param upnpRulesList List<UpnpRule>
     * @return LinkedList<RuleGeneric>
     */
    public static LinkedList<RuleGeneric> transformUpnpRulesToGeneric(List<UpnpRule> upnpRulesList) {
        LinkedList<RuleGeneric> upnpRulesGenList = new LinkedList<>();
        for (UpnpRule rule : upnpRulesList) {
            RuleGeneric upnpRuleGen = getUpnpRuleGeneric(rule);
            upnpRulesGenList.add(upnpRuleGen);
        }
        return upnpRulesGenList;
    }

    /**
     * Transforms a UpnpRule object into its generic RuleGeneric representation.
     * @param ruleLegacy UpnpRule
     * @return RuleGeneric
     */
    private static RuleGeneric getUpnpRuleGeneric(UpnpRule ruleLegacy) {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(ruleLegacy.getEnabled());
        String ruleId = ruleLegacy.getRuleId() != null ? String.valueOf(ruleLegacy.getRuleId()) : null;

        // Transform Action
        RuleActionGeneric ruleActionGen = new RuleActionGeneric();
        ruleActionGen.setType(Boolean.TRUE.equals(ruleLegacy.getAllow()) ? RuleActionGeneric.Type.ACCEPT : RuleActionGeneric.Type.REJECT);

        // Transform Conditions
        LinkedList<RuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        for (UpnpRuleCondition ruleCondition : ruleLegacy.getConditions()) {
            String op = ruleCondition.getInvert() ? Constants.IS_NOT_EQUALS_TO : Constants.IS_EQUALS_TO;
            RuleConditionGeneric ruleConditionGen = new RuleConditionGeneric(op, ruleCondition.getConditionType(), ruleCondition.getValue());
            ruleConditionGenList.add(ruleConditionGen);
        }

        // Create Generic Rule
        RuleGeneric upnpRuleGen = new RuleGeneric(enabled, ruleLegacy.getDescription(), ruleId);
        upnpRuleGen.setAction(ruleActionGen);
        upnpRuleGen.setConditions(ruleConditionGenList);
        return upnpRuleGen;
    }
}

