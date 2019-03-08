/**
 * $Id$
 */
package com.untangle.app.shield;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This in the implementation of a Shield Rule
 *
 * A rule is basically a collection of ShieldRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class ShieldRule implements JSONString, Serializable
{
    private static final Logger logger = Logger.getLogger(ShieldRule.class);

    public enum ShieldRuleAction
    {
        SCAN, PASS
    }

    private List<ShieldRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private String description;
    private ShieldRuleAction action = ShieldRuleAction.SCAN;
    
    public ShieldRule()
    {
    }

    public ShieldRule(boolean enabled, List<ShieldRuleCondition> matchers, boolean flag, boolean block, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setDescription(description);
    }
    
    public List<ShieldRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<ShieldRuleCondition> newValue ) { this.matchers = newValue; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public ShieldRuleAction getAction() { return action; }
    public void setAction( ShieldRuleAction newValue ) { this.action = newValue; }
    
    /**
     * DEPRECATED in v13 - 13.0 conversion
     * Multiplier has been remove in v13
     * It is kept here for now so that JSON serialization still works and the settings can be converted
     * This can be removed after v13
     */
    private int multiplier = 1;
    public int getMultiplier() { return multiplier; }
    public void setMultiplier( int newValue ) { this.multiplier = newValue; }

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
         * If no matchers return true
         */
        if (this.matchers == null) {
            logger.warn("Null matchers - assuming true");
            return true;
        }

        /**
         * IF any matcher doesn't match - return false
         */
        for ( ShieldRuleCondition matcher : matchers ) {
            if (!matcher.matches(protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort ))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
    
}

