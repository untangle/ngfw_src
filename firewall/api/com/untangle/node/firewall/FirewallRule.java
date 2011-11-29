/*
 * $Id$
 */
package com.untangle.node.firewall;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.vnet.IPSession;

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

    private Integer id;
    private Boolean enabled;
    private Boolean log;
    private Boolean block;
    private String description;
    
    public FirewallRule()
    {
    }

    public FirewallRule(boolean enabled, List<FirewallRuleMatcher> matchers, boolean log, boolean block, String description)
    {
        this.setMatchers(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setLog(Boolean.valueOf(log));
        this.setBlock(Boolean.valueOf(block));
        this.setDescription(description);
    }
    
    public List<FirewallRuleMatcher> getMatchers()
    {
        return this.matchers;
    }

    public void setMatchers( List<FirewallRuleMatcher> matchers )
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

    public Boolean getBlock()
    {
        return block;
    }

    public void setBlock( Boolean block )
    {
        this.block = block;
    }

    public Boolean getLog()
    {
        return log;
    }

    public void setLog( Boolean log )
    {
        this.log = log;
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
                            int srcPort, int dstPort,
                            String username)
    {
        if (!getEnabled())
            return false;

        //logger.debug("Checking rule " + getId() + " against [" + protocol + " " + srcAddress + ":" + srcPort + " -> " + dstAddress + ":" + dstPort + " (" + username + ")]");
            
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

