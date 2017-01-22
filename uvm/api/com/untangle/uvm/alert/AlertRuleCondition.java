/**
 * $Id: AlertRuleCondition.java 37267 2014-02-26 23:42:19Z dmorris $
 */
package com.untangle.uvm.alert;

import org.json.JSONObject;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.RuleCondition;

/**
 * This is a matching criteria for a Alert Control Rule
 *
 * A AlertRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class AlertRuleCondition
{
    private static final Logger logger = Logger.getLogger( AlertRuleCondition.class );

    private ConditionType matcherType;
    private AlertRuleConditionField value;
    
    public enum ConditionType {
        FIELD_CONDITION
    };
    
    public AlertRuleCondition()
    {
    }

    public AlertRuleCondition( ConditionType matcherType, AlertRuleConditionField value )
    {
        this.matcherType = matcherType;
        this.value = value;
    }

    public ConditionType getConditionType() { return matcherType; }
    public void setConditionType( ConditionType newValue ) { this.matcherType = newValue; }

    public AlertRuleConditionField getValue() { return value; }
    public void setValue( AlertRuleConditionField newValue ) { this.value = newValue; }

    public boolean isMatch( JSONObject obj )
    {
        if ( value == null )
            return false;
        
        return value.isMatch( obj );
    }

}
