/**
 * $Id$
 */
package com.untangle.uvm.network;

import com.untangle.uvm.app.RuleCondition;


/**
 * This is a matching criteria for a NatRule Rule
 * Example: "Destination Port" == "80"
 *
 * A NatRule has a set of these to determine what traffic to match
 */
@SuppressWarnings("serial")
public class NatRuleCondition extends RuleCondition
{
    public NatRuleCondition( )
    {
        super();
    }

    public NatRuleCondition( ConditionType matcherType, String value )
    {
        super( matcherType, value );
    }

    public NatRuleCondition( ConditionType matcherType, String value, Boolean invert )
    {
        super( matcherType, value, invert );
    }
}
