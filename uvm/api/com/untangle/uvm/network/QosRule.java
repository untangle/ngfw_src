/**
 * $Id: QosRule.java 33317 2012-10-17 19:12:21Z dmorris $
 */
package com.untangle.uvm.network;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This in the implementation of a Qos Rule
 *
 * A rule is basically a collection of QosRuleMatchers (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class QosRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<QosRuleMatcher> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private String description;

    private int priority;
    
    public QosRule() { }

    public QosRule(boolean enabled, String description, List<QosRuleMatcher> matchers, int priority)
    {
        this.setMatchers(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setPriority(priority);
    }
    
    public List<QosRuleMatcher> getMatchers() { return this.matchers; }
    public void setMatchers( List<QosRuleMatcher> matchers ) { this.matchers = matchers; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public int getPriority() { return priority; }
    public void setPriority( int newValue ) { this.priority = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

