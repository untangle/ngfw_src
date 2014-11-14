/**
 * $Id: AlertRuleMatcher.java 37267 2014-02-26 23:42:19Z dmorris $
 */
package com.untangle.node.reporting;

import org.json.JSONObject;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.RuleMatcher;

/**
 * This is a matching criteria for a Alert Control Rule
 *
 * A AlertRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class AlertRuleMatcher
{
    private static final Logger logger = Logger.getLogger( AlertRuleMatcher.class );

    private MatcherType matcherType;
    private AlertRuleMatcherField value;
    
    public enum MatcherType {
        FIELD_CONDITION
    };
    
    public AlertRuleMatcher()
    {
    }

    public AlertRuleMatcher( MatcherType matcherType, AlertRuleMatcherField value )
    {
        this.matcherType = matcherType;
        this.value = value;
    }

    public MatcherType getMatcherType() { return matcherType; }
    public void setMatcherType( MatcherType newValue ) { this.matcherType = newValue; }

    public AlertRuleMatcherField getValue() { return value; }
    public void setValue( AlertRuleMatcherField newValue ) { this.value = newValue; }

    public boolean isMatch( JSONObject obj )
    {
        if ( value == null )
            return false;
        
        return value.isMatch( obj );
    }

}
