/*
 * $Id: BypassRule.java 33317 2012-10-17 19:12:21Z dmorris $
 */
package com.untangle.uvm.network;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeIPSession;

/**
 * This in the implementation of a Bypass Rule
 *
 * A rule is basically a collection of BypassRuleMatchers (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class BypassRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<BypassRuleMatcher> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean bypass;
    private String description;
    
    public BypassRule()
    {
    }

    public BypassRule(boolean enabled, List<BypassRuleMatcher> matchers, boolean bypass, String description)
    {
        this.setMatchers(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setBypass(bypass);
        this.setDescription(description);
    }
    
    public List<BypassRuleMatcher> getMatchers()
    {
        return this.matchers;
    }

    public void setMatchers( List<BypassRuleMatcher> matchers )
    {
        this.matchers = matchers;
    }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public Boolean getBypass() { return bypass; }
    public void setBypass( Boolean bypass ) { this.bypass = bypass; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

