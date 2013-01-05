/*
 * $Id: NatRule.java 33317 2012-10-17 19:12:21Z dmorris $
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
 * This in the implementation of a Nat Rule
 *
 * A rule is basically a collection of NatRuleMatchers (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class NatRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<NatRuleMatcher> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean auto;
    private InetAddress newSource;
    private String description;
    
    public NatRule()
    {
    }

    public NatRule(boolean enabled, List<NatRuleMatcher> matchers, InetAddress newSource, String description)
    {
        this.setMatchers(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setNewSource(newSource);
        this.setDescription(description);
    }
    
    public List<NatRuleMatcher> getMatchers()
    {
        return this.matchers;
    }

    public void setMatchers( List<NatRuleMatcher> matchers )
    {
        this.matchers = matchers;
    }

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

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

