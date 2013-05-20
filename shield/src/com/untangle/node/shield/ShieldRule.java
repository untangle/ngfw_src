/*
 * $Id: ShieldRule.java 34439 2013-04-01 21:11:16Z dmorris $
 */
package com.untangle.node.shield;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeSession;

/**
 * This in the implementation of a Shield Rule
 *
 * A rule is basically a collection of ShieldRuleMatchers (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class ShieldRule implements JSONString, Serializable
{
    private static final Logger logger = Logger.getLogger(ShieldRule.class);

    private List<ShieldRuleMatcher> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private int multiplier = 1;
    private String description;
    
    public ShieldRule()
    {
    }

    public ShieldRule(boolean enabled, List<ShieldRuleMatcher> matchers, boolean flag, boolean block, String description)
    {
        this.setMatchers(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setDescription(description);
    }
    
    public List<ShieldRuleMatcher> getMatchers() { return this.matchers; }
    public void setMatchers( List<ShieldRuleMatcher> newValue ) { this.matchers = newValue; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public int getMultiplier() { return multiplier; }
    public void setMultiplier( int newValue ) { this.multiplier = newValue; }
    
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
                            String username)
    {
        if (!getEnabled())
            return false;

        //logger.debug("Checking rule " + getRuleId() + " against [" + protocol + " " + srcAddress + ":" + srcPort + " -> " + dstAddress + ":" + dstPort + " (" + username + ")]");
            
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
        for ( ShieldRuleMatcher matcher : matchers ) {
            if (!matcher.matches(protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort, username))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
    
}

