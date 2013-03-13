/**
 * $Id: FilterRule.java 33317 2012-10-17 19:12:21Z dmorris $
 */
package com.untangle.uvm.network;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This in the implementation of a Filter Rule
 *
 * A rule is basically a collection of FilterRuleMatchers (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class FilterRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<FilterRuleMatcher> matchers;

    private Integer ruleId;
    private boolean enabled = true;
    private boolean blocked = false;
    private String description;
    
    public FilterRule() { }

    public FilterRule(boolean enabled, List<FilterRuleMatcher> matchers, boolean blocked, String description)
    {
        this.setMatchers(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setBlocked(blocked);
        this.setDescription(description);
    }
    
    public List<FilterRuleMatcher> getMatchers() { return this.matchers; }
    public void setMatchers( List<FilterRuleMatcher> matchers ) { this.matchers = matchers; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public boolean getBlocked() { return blocked; }
    public void setBlocked( boolean newDestination ) { this.blocked = newDestination; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

