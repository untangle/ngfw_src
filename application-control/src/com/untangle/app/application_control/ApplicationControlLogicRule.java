/**
 * $Id: ApplicationControlLogicRule.java 37269 2014-02-26 23:46:16Z dmorris $
 */
package com.untangle.app.application_control;

import java.util.List;
import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.vnet.AppSession;

/**
 * This in the implementation of a ApplicationControlLogicRule
 * 
 * A rule is basically a collection of ApplicationControlLogicRuleConditions (matchers) and a
 * ApplicationControlLogicRuleAction (action) to be taken if they match
 */

@SuppressWarnings("serial")
public class ApplicationControlLogicRule implements JSONString, Serializable
{
    private final Logger logger = LogManager.getLogger(getClass());

    private List<ApplicationControlLogicRuleCondition> matchers;
    private ApplicationControlLogicRuleAction action;

    private int id;
    private boolean enabled;
    private String description;

    public ApplicationControlLogicRule()
    {
    }

    public List<ApplicationControlLogicRuleCondition> getConditions() { return this.matchers; }
    public void setConditions(List<ApplicationControlLogicRuleCondition> matchers) { this.matchers = matchers; }

    public int getId() { return this.id; }
    public void setId(int id) { this.id = id; }

    public ApplicationControlLogicRuleAction getAction() { return this.action; }
    public void setAction(ApplicationControlLogicRuleAction action) { this.action = action; }

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    /* deprecated - live renamed to enabled - this remains so json serialization works */
    public void setLive(boolean live) { this.enabled = live; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public boolean matches(AppSession sess)
    {
        if (!getEnabled())
            return false;

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
        for (ApplicationControlLogicRuleCondition matcher : matchers) {
            if (!matcher.matches(sess))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
}
