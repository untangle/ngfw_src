/**
 * $Id: SslInspectorRule.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.ssl_inspector;

import java.util.List;
import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AppSession;

/**
 * This in the implementation of an SSL Inspector rule.
 * 
 * A rule is basically a collection of rule conditions (matchers) and an action
 * that defines what to do if all the matchers match.
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class SslInspectorRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<SslInspectorRuleCondition> matchers;
    private SslInspectorRuleAction action;

    private int ruleId;
    private boolean enabled;
    private String description;

    public SslInspectorRule()
    {
    }

    // THIS IS FOR ECLIPSE - @formatter:off
    
    public List<SslInspectorRuleCondition> getConditions() { return this.matchers; }
    public void setConditions(List<SslInspectorRuleCondition> matchers) { this.matchers = matchers; }

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    /* deprecated - live renamed to enabled - this remains so json serialization works */
    public void setLive(boolean live) { this.enabled = live; }
    
    public int getRuleId() { return this.ruleId; }
    public void setRuleId(int ruleId) { this.ruleId = ruleId; }

    public SslInspectorRuleAction getAction() { return this.action; }
    public void setAction(SslInspectorRuleAction action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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
         * If no matchers return true
         */
        if (this.matchers == null) {
            logger.warn("Null matchers - assuming true");
            return true;
        }

        /**
         * If any matcher doesn't match - return false
         */
        for (SslInspectorRuleCondition matcher : matchers) {
            if (!matcher.matches(sess)) return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
}
