/**
 * $Id$
 */
package com.untangle.app.policy_manager;

import com.untangle.uvm.app.RuleCondition;


/**
 * This is a matching criteria for a Policy Rule
 * Example: "Destination Port" == "80"
 *
 * A PolicyRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class PolicyRuleCondition extends RuleCondition
{
    /**
     * PolicyRuleCondition creates a new PolicyRuleCondition
     */
    public PolicyRuleCondition( )
    {
        super();
    }

    /**
     * PolicyRuleCondition creates a new PolicyRuleCondition
     * @param matcherType
     * @param value
     */
    public PolicyRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    /**
     * PolicyRuleCondition creates a new PolicyRuleCondition
     * @param matcherType
     * @param value
     * @param invert
     */
    public PolicyRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
