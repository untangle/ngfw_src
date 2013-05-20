/**
 * $Id$
 */
package com.untangle.node.firewall;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeSession;

/**
 * This in the implementation of a Firewall Rule
 *
 * A rule is basically a collection of FirewallRuleMatchers (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class FirewallRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<FirewallRuleMatcher> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean flag;
    private Boolean block;
    private String description;
    
    public FirewallRule()
    {
    }

    public FirewallRule(boolean enabled, List<FirewallRuleMatcher> matchers, boolean flag, boolean block, String description)
    {
        this.setMatchers(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setFlag(Boolean.valueOf(flag));
        this.setBlock(Boolean.valueOf(block));
        this.setDescription(description);
    }
    
    public List<FirewallRuleMatcher> getMatchers() { return this.matchers; }
    public void setMatchers( List<FirewallRuleMatcher> newValue ) { this.matchers = newValue; }

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
        for ( FirewallRuleMatcher matcher : matchers ) {
            if (!matcher.matches(protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort, username))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
    
}

