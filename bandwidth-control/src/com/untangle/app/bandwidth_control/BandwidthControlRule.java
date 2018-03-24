/**
 * $Id$
 */
package com.untangle.app.bandwidth_control;

import java.util.List;
import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AppSession;

/**
 * This in the implementation of a Bandwidth Rule
 *
 * A rule is basically a collection of BandwidthControlRuleConditions (matchers)
 * and a BandwidthControlRuleAction (action) to be taken if they match
 */
@SuppressWarnings("serial")
public class BandwidthControlRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<BandwidthControlRuleCondition> matchers;
    private BandwidthControlRuleAction action;

    private int ruleId;
    private boolean live;
    private String description;
    
    public BandwidthControlRule() { }

    public List<BandwidthControlRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<BandwidthControlRuleCondition> newValue ) { this.matchers = newValue; }

    public int getRuleId() { return this.ruleId; }
    public void setRuleId(int newValue) { this.ruleId = newValue; }
    
    public boolean getEnabled() { return live; }
    public void setEnabled( boolean newValue ) { this.live = newValue; }

    public String getDescription() { return description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public BandwidthControlRuleAction getAction()
    {
        return this.action;
    }

    public void setAction( BandwidthControlRuleAction action )
    {
        this.action = action;
        this.action.setRule(this);
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
    public boolean matches( AppSession sess )
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
        for ( BandwidthControlRuleCondition matcher : matchers ) {
            if (!matcher.matches(sess))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
    
}
