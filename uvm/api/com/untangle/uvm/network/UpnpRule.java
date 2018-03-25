/**
 * $Id: UpnpRule.java 41565 2016-07-27 22:13:52Z cblaise $
 */
package com.untangle.uvm.network;

import java.util.List;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This in the implementation of a Upnp Rule
 *
 * A rule is basically a collection of UpnpRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class UpnpRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<UpnpRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private String description;
    private Boolean allow;

    public UpnpRule() { }

    public UpnpRule(boolean enabled, String description, List<UpnpRuleCondition> matchers, Boolean allow)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setAllow(Boolean.valueOf(allow));
    }
    
    public List<UpnpRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<UpnpRuleCondition> matchers ) { this.matchers = matchers; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public Boolean getAllow() { return allow; }
    public void setAllow( Boolean allow ) { this.allow = allow; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

