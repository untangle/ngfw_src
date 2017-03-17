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
    public ShieldRuleCondition( )
    {
        super();
    }

    public ShieldRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public ShieldRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
