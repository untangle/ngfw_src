/**
 * $Id: PortForwardRule.java 33317 2012-10-17 19:12:21Z dmorris $
 */
package com.untangle.uvm.network;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This in the implementation of a PortForward Rule
 *
 * A rule is basically a collection of PortForwardRuleMatchers (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class PortForwardRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<PortForwardRuleMatcher> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private InetAddress newDestination;
    private Integer newPort;
    private String description;
    private Boolean simple;
    
    public PortForwardRule() { }

    public PortForwardRule(boolean enabled, List<PortForwardRuleMatcher> matchers, InetAddress newDestination, Integer newPort, String description)
    {
        this.setMatchers(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setNewDestination(newDestination);
        this.setNewPort(newPort);
        this.setDescription(description);
        this.setSimple(false);
    }
    
    public List<PortForwardRuleMatcher> getMatchers() { return this.matchers; }
    public void setMatchers( List<PortForwardRuleMatcher> matchers ) { this.matchers = matchers; }

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
}

