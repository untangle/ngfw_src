/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.util.LinkedList;
import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import com.untangle.uvm.generic.RuleActionGeneric;
import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.util.Constants;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This in the implementation of a Nat Rule
 *
 * A rule is basically a collection of NatRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class NatRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<NatRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean auto;
    private InetAddress newSource;
    private String description;
    private boolean ngfwAdded = false;
    private String addedBy;
    
    public NatRule() { }

    public NatRule(boolean enabled, List<NatRuleCondition> matchers, InetAddress newSource, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setNewSource(newSource);
        this.setDescription(description);
        this.setAddedBy("user-created");
    }

    public List<NatRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<NatRuleCondition> matchers ) { this.matchers = matchers; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public Boolean getAuto() { return auto; }
    public void setAuto( Boolean auto ) { this.auto = auto; }

    public InetAddress getNewSource() { return newSource; }
    public void setNewSource( InetAddress newSource ) { this.newSource = newSource; }

    public boolean getNgfwAdded() { return this.ngfwAdded; }
    public void setNgfwAdded( boolean ngfwAdded ) { this.ngfwAdded = ngfwAdded; }

    public String getAddedBy() { return this.addedBy; }
    public void setAddedBy( String addedBy ) { this.addedBy = addedBy; } 

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of NatRule objects into their generic RuleGeneric representation.
     * Used for get api calls
     * @param natRulesList List<NatRule>
     * @return LinkedList<RuleGeneric>
     */
    public static LinkedList<RuleGeneric> transformNatRulesToGeneric(List<NatRule> natRulesList) {
        LinkedList<RuleGeneric> natRulesGenList = new LinkedList<>();
        for (NatRule rule : natRulesList) {
            RuleGeneric natRulesGen = getNatRuleGeneric(rule);
            natRulesGenList.add(natRulesGen);
        }
        return natRulesGenList;
    }

    /**
     * Transforms a NatRule object into its generic RuleGeneric representation.
     * @param rule NatRule
     * @return RuleGeneric
     */
    private static RuleGeneric getNatRuleGeneric(NatRule rule) {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(rule.getEnabled());
        String ruleId = rule.getRuleId() != null ? String.valueOf(rule.getRuleId()) : null;

        // Transform Action
        RuleActionGeneric ruleActionGen = new RuleActionGeneric();
        ruleActionGen.setType(rule.getAuto() ? RuleActionGeneric.Type.MASQUERADE : RuleActionGeneric.Type.SNAT);
        ruleActionGen.setSnat_address(rule.getNewSource());

        // Transform Conditions
        LinkedList<RuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        for (NatRuleCondition ruleCondition : rule.getConditions()) {
            String op = ruleCondition.getInvert() ? Constants.IS_NOT_EQUALS_TO : Constants.IS_EQUALS_TO;
            RuleConditionGeneric ruleConditionGen = new RuleConditionGeneric(op, ruleCondition.getConditionType(), ruleCondition.getValue());
            ruleConditionGenList.add(ruleConditionGen);
        }

        // Create Generic Rule
        RuleGeneric natRulesGen = new RuleGeneric(enabled, rule.getDescription(), ruleId);
        natRulesGen.setAction(ruleActionGen);
        natRulesGen.setConditions(ruleConditionGenList);
        return natRulesGen;
    }
}

