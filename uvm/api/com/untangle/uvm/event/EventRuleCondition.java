/**
 * $Id: EventRuleCondition.java 37267 2014-02-26 23:42:19Z dmorris $
 */
package com.untangle.uvm.event;

import org.json.JSONObject;

import org.apache.log4j.Logger;

import com.untangle.uvm.app.RuleCondition;

/**
 * This is a matching criteria for a Event Control Rule
 *
 * A EventRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class EventRuleCondition
{
    private static final Logger logger = Logger.getLogger( EventRuleCondition.class );

    private ConditionType matcherType;
    private EventRuleConditionField value;
    
    public enum ConditionType {
        FIELD_CONDITION
    };
    
    public EventRuleCondition()
    {
    }

    public EventRuleCondition( ConditionType matcherType, EventRuleConditionField value )
    {
        this.matcherType = matcherType;
        this.value = value;
    }

    public ConditionType getConditionType() { return matcherType; }
    public void setConditionType( ConditionType newValue ) { this.matcherType = newValue; }

    public EventRuleConditionField getValue() { return value; }
    public void setValue( EventRuleConditionField newValue ) { this.value = newValue; }

    public boolean isMatch( JSONObject obj )
    {
        if ( value == null )
            return false;
        
        return value.isMatch( obj );
    }

}
