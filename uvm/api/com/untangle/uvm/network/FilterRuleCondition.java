/**
 * $Id$
 */
package com.untangle.uvm.network;

import com.untangle.uvm.app.RuleCondition;


/**
 * This is a matching criteria for a Filter Rule
 * Example: "Destination Port" == "80"
 *
 * A FilterRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class FilterRuleCondition extends RuleCondition implements java.io.Serializable, org.json.JSONString
{
    public FilterRuleCondition( )
    {
        super();
    }

    public FilterRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public FilterRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }

    public String toJSONString()
    {
        return (new org.json.JSONObject(this)).toString();
    }
}
