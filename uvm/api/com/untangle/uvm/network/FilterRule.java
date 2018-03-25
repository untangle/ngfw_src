/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.util.List;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This in the implementation of a Filter Rule
 *
 * A rule is basically a collection of FilterRuleConditions (conditions)
 * and what to do if the conditions match (block, log, etc)
 */
@SuppressWarnings("serial")
public class FilterRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<FilterRuleCondition> conditions;

    private Integer ruleId;
    private boolean enabled = true;
    private boolean ipv6Enabled = true;
    private boolean blocked = false;
    private Boolean readOnly = null;
    private String description;
    
    public FilterRule() { }

    public FilterRule(boolean enabled, boolean ipv6Enabled, List<FilterRuleCondition> conditions, boolean blocked, String description)
    {
        this.setConditions(conditions);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setIpv6Enabled(Boolean.valueOf(ipv6Enabled));
        this.setBlocked(blocked);
        this.setDescription(description);
    }
    
    public List<FilterRuleCondition> getConditions() { return this.conditions; }
    public void setConditions( List<FilterRuleCondition> conditions ) { this.conditions = conditions; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public boolean getEnabled() { return enabled; }
    public void setEnabled( boolean enabled ) { this.enabled = enabled; }

    public boolean getIpv6Enabled() { return ipv6Enabled; }
    public void setIpv6Enabled( boolean ipv6Enabled ) { this.ipv6Enabled = ipv6Enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public boolean getBlocked() { return blocked; }
    public void setBlocked( boolean newDestination ) { this.blocked = newDestination; }

    public Boolean getReadOnly() { return this.readOnly; }
    public void setReadOnly( Boolean newValue ) { this.readOnly = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

