/**
 * $Id$
 */
package com.untangle.app.policy_manager;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This in the implementation of a Policy Rule
 *
 * A rule is basically a collection of PolicyRuleConditions (matchers)
 * and what to do if the matchers match (targetPolicy)
 */
@SuppressWarnings("serial")
public class PolicyRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<PolicyRuleCondition> conditions;

    private Integer ruleId;
    private Boolean enabled;
    private String description;
    private Integer targetPolicy;
    
    public PolicyRule()
    {
    }

    public PolicyRule(boolean enabled, List<PolicyRuleCondition> conditions, Integer targetPolicy, String description)
    {
        this.setConditions(conditions);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setTargetPolicy(targetPolicy);
        this.setDescription(description);
    }
    
    public List<PolicyRuleCondition> getConditions() { return this.conditions; }
    public void setConditions( List<PolicyRuleCondition> conditions ) { this.conditions = conditions; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public Integer getTargetPolicy() { return targetPolicy; }
    public void setTargetPolicy( Integer targetPolicy ) { this.targetPolicy = targetPolicy; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
    public boolean isMatch( short protocol,
                            int srcIntf, int dstIntf,
                            InetAddress srcAddress, InetAddress dstAddress,
                            int srcPort, int dstPort)
    {
        if (!getEnabled())
            return false;

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
        for ( PolicyRuleCondition matcher : conditions ) {
            if (!matcher.matches( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort, null))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
    
}

