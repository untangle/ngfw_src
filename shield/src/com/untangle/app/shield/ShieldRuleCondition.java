/**
 * $Id$
 */
package com.untangle.app.shield;

import com.untangle.uvm.app.RuleCondition;


/**
 * This is a matching criteria for a Shield Rule
 * Example: "Destination Port" == "80"
 *
 * A ShieldRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class ShieldRuleCondition extends RuleCondition
{
    /**
     * ShieldRuleCondition creates a new ShieldRuleCondition
     */
    public ShieldRuleCondition( )
    {
        super();
    }

    /**
     * ShieldRuleCondition creates a new ShieldRuleCondition
     * @param matcherType
     * @param value
     */
    public ShieldRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    /**
     * ShieldRuleCondition creates a new ShieldRuleCondition
     * @param matcherType
     * @param value
     * @param invert
     */
    public ShieldRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
