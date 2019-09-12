/**
 * $Id$
 */
package com.untangle.app.ip_reputation;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.IPNewSessionRequest;

/**
 * This in the implementation of an IP Reputation Pass Rule
 *
 * A rule is basically a collection of IpReputationPassRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class IpReputationPassRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<IpReputationPassRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean flag;
    private Boolean pass;
    private String description;
    
    public IpReputationPassRule()
    {
    }

    public IpReputationPassRule(boolean enabled, List<IpReputationPassRuleCondition> matchers, boolean flag, boolean pass, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setFlag(Boolean.valueOf(flag));
        this.setPass(Boolean.valueOf(pass));
        this.setDescription(description);
    }
    
    public List<IpReputationPassRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<IpReputationPassRuleCondition> newValue ) { this.matchers = newValue; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public Boolean getPass() { return pass; }
    public void setPass( Boolean newValue ) { this.pass = newValue; }

    public Boolean getFlag() { return flag; }
    public void setFlag( Boolean newValue ) { this.flag = newValue; }
    
    public String getDescription() { return description; }
    public void setDescription( String newValue ) { this.description = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
    public boolean isMatch( IPNewSessionRequest request)
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
        for ( IpReputationPassRuleCondition matcher : matchers ) {
            if (!matcher.matches(request.getProtocol(),
                             request.getClientIntf(), request.getServerIntf(),
                             request.getOrigClientAddr(), request.getNewServerAddr(),
                             request.getOrigClientPort(), request.getNewServerPort(),
                             request))
            return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
    
}

