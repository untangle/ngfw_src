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
 * This in the implementation of a PortForward Rule
 *
 * A rule is basically a collection of PortForwardRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class PortForwardRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<PortForwardRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private InetAddress newDestination;
    private Integer newPort;
    private String description;
    private Boolean simple;
    
    public PortForwardRule() { }

    public PortForwardRule(boolean enabled, List<PortForwardRuleCondition> matchers, InetAddress newDestination, Integer newPort, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setNewDestination(newDestination);
        this.setNewPort(newPort);
        this.setDescription(description);
        this.setSimple(false);
    }

    public List<PortForwardRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<PortForwardRuleCondition> matchers ) { this.matchers = matchers; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public InetAddress getNewDestination() { return newDestination; }
    public void setNewDestination( InetAddress newDestination ) { this.newDestination = newDestination; }

    public Integer getNewPort() { return newPort; }
    public void setNewPort( Integer newPort ) { this.newPort = newPort; }
    
    public Boolean getSimple() { return simple;}
    public void setSimple( Boolean simple ) { this.simple = simple; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of PortForwardRule objects into their generic RuleGeneric representation.
     * Used for get api calls
     * @param portForwardRuleList List<PortForwardRule>
     * @return LinkedList<RuleGeneric>
     */
    public static LinkedList<RuleGeneric> transformPortForwardRulesToGeneric(List<PortForwardRule> portForwardRuleList) {
        LinkedList<RuleGeneric> portForwardRulesGenList = new LinkedList<>();
        for (PortForwardRule rule : portForwardRuleList) {
            RuleGeneric portForwardRulesGen = getPortForwardRuleGeneric(rule);
            portForwardRulesGenList.add(portForwardRulesGen);
        }
        return portForwardRulesGenList;
    }

    /**
     * Transforms a PortForwardRule object into its generic RuleGeneric representation.
     * @param rule PortForwardRule
     * @return RuleGeneric
     */
    private static RuleGeneric getPortForwardRuleGeneric(PortForwardRule rule) {
        // Transform enabled and ruleId
        boolean enabled = Boolean.TRUE.equals(rule.getEnabled());
        String ruleId = rule.getRuleId() != null ? String.valueOf(rule.getRuleId()) : null;

        // Transform Action
        RuleActionGeneric ruleActionGen = new RuleActionGeneric();
        ruleActionGen.setType(RuleActionGeneric.Type.DNAT);
        ruleActionGen.setDnat_address(rule.getNewDestination());
        ruleActionGen.setDnat_port(rule.getNewPort() != null ? String.valueOf(rule.getNewPort()) : null);

        // Transform Conditions
        LinkedList<RuleConditionGeneric> ruleConditionGenList = new LinkedList<>();
        for (PortForwardRuleCondition ruleCondition : rule.getConditions()) {
            String op = ruleCondition.getInvert() ? Constants.IS_NOT_EQUALS_TO : Constants.IS_EQUALS_TO;
            RuleConditionGeneric ruleConditionGen = new RuleConditionGeneric(op, ruleCondition.getConditionType(), ruleCondition.getValue());
            ruleConditionGenList.add(ruleConditionGen);
        }

        // Create Generic Rule
        RuleGeneric portForwardRulesGen = new RuleGeneric(enabled, rule.getDescription(), ruleId);
        portForwardRulesGen.setAction(ruleActionGen);
        portForwardRulesGen.setConditions(ruleConditionGenList);
        return portForwardRulesGen;
    }
}

