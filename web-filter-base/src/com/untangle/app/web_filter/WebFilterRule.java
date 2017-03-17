/**
 * $Id: WebFilterRule.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.web_filter;

import java.util.List;
import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AppSession;

/**
 * This in the implementation of a WebFilterRule
 * 
 * A rule is basically a collection of WebFilterRuleConditions and booleans for
 * flagged and blocked to set action to be taken if they match
 */

@SuppressWarnings("serial")
public class WebFilterRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<WebFilterRuleCondition> conditions = null;
    private String description = null;
    public boolean enabled;
    public boolean flagged;
    public boolean blocked;
    private int ruleId;

    public WebFilterRule()
    {
    }

    // THIS IS FOR ECLIPSE - @formatter:off
    
    public List<WebFilterRuleCondition> getConditions() { return this.conditions; }
    public void setConditions(List<WebFilterRuleCondition> conditions) { this.conditions = conditions; }

    public int getRuleId() { return this.ruleId; }
    public void setRuleId(int argval) { this.ruleId = argval; }

    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean argval) { this.enabled = argval; }

    public boolean getFlagged() { return flagged; }
    public void setFlagged(boolean argval) { this.flagged = argval; }

    public boolean getBlocked() { return this.blocked; }
    public void setBlocked(boolean argval) { this.blocked = argval; }
    
    public String getDescription() { return description; }
    public void setDescription(String argstr) { this.description = argstr; }

    // THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public boolean matches(AppSession sess)
    {
        if (!getEnabled()) return false;

        /**
         * If no conditions return true
         */
        if (this.conditions == null) {
            logger.warn("Null conditions - assuming true");
            return true;
        }

        /**
         * IF any matcher doesn't match - return false
         */
        for (WebFilterRuleCondition item : conditions) {
            if (!item.matches(sess)) return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
}
