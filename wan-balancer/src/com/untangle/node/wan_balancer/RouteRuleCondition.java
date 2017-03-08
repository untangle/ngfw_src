/**
 * $Id$
 */
package com.untangle.node.wan_balancer;

import com.untangle.uvm.node.RuleCondition;


/**
 * This is a matching criteria for a RouteRule Rule
 * Example: "Destination Port" == "80"
 *
 * A RouteRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class RouteRuleCondition extends RuleCondition
{
    public RouteRuleCondition( )
    {
        super();
    }

    public RouteRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public RouteRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
