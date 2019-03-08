/**
 * $Id$
 */

package com.untangle.app.wan_balancer;

import com.untangle.uvm.app.RuleCondition;

/**
 * This is a matching criteria for a RouteRule Rule
 * Example: "Destination Port" == "80"
 * 
 * A RouteRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class RouteRuleCondition extends RuleCondition
{
    /**
     * Constructor
     */
    public RouteRuleCondition()
    {
        super();
    }

    /**
     * Constructor
     * 
     * @param matcherType
     *        The matcher type
     * @param value
     *        The value
     */
    public RouteRuleCondition(ConditionType matcherType, String value)
    {
        super(matcherType, value);
    }

    /**
     * Constructor
     * 
     * @param matcherType
     *        The matcher type
     * @param value
     *        The value
     * @param invert
     *        Invert flag
     */
    public RouteRuleCondition(ConditionType matcherType, String value, Boolean invert)
    {
        super(matcherType, value, invert);
    }
}
