/*
 * $Id$
 */
package com.untangle.node.policy_manager;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeSession;

/**
 * This in the implementation of a Policy Rule
 *
 * A rule is basically a collection of PolicyRuleMatchers (matchers)
 * and what to do if the matchers match (targetPolicy)
 */
@SuppressWarnings("serial")
public class PolicyRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<PolicyRuleMatcher> matchers;

    private Integer id;
    private Boolean enabled;
    private String description;
    private Long targetPolicy;
    
    public PolicyRule()
    {
    }

    public PolicyRule(boolean enabled, List<PolicyRuleMatcher> matchers, Long targetPolicy, String description)
    {
        this.setMatchers(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setTargetPolicy(targetPolicy);
        this.setDescription(description);
    }
    
    public List<PolicyRuleMatcher> getMatchers()
    {
        return this.matchers;
    }

    public void setMatchers( List<PolicyRuleMatcher> matchers )
    {
        this.matchers = matchers;
    }

    public Integer getId()
    {
        return this.id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }
    
    public Boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled( Boolean enabled )
    {
        this.enabled = enabled;
    }

    public Long getTargetPolicy()
    {
        return targetPolicy;
    }

    public void setTargetPolicy( Long targetPolicy )
    {
        this.targetPolicy = targetPolicy;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }
    
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
        for ( PolicyRuleMatcher matcher : matchers ) {
            if (!matcher.matches( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort ))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
    
}

