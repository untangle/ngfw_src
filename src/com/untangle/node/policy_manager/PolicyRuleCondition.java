/**
 * $Id$
 */
package com.untangle.node.policy_manager;

import com.untangle.uvm.node.RuleCondition;


/**
 * This is a matching criteria for a Policy Rule
 * Example: "Destination Port" == "80"
 *
 * A PolicyRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class PolicyRuleCondition extends RuleCondition
{
    public PolicyRuleCondition( )
    {
        super();
    }

    public PolicyRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public PolicyRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
